import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * จุดเริ่มต้นของโปรแกรม
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ส่วนที่เขียนไว้ให้แล้วคือการรับค่า การโหลด workload และการแสดง error
 * ซึ่งไม่ใช่สิ่งที่โครงงานนี้วัด ส่วนที่เหลือเป็น TODO ทั้งหมด
 *
 * วิธีรัน:
 *   java Main jobs_standard.csv priority 3 1 2
 */
public class Main {

    public static void main(String[] args) {
        // ---------- 1. รับค่าจาก command line ----------
        Config config;
        try {
            config = Config.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ผิดพลาด: " + e.getMessage());
            System.err.println();
            System.err.println(Config.USAGE);
            System.exit(1);
            return;
        }

        // ---------- 2. เริ่มจับเวลาและโหลด workload ----------
        ProjectLogger logger = new ProjectLogger();
        List<Job> jobs;
        try {
            jobs = WorkloadLoader.load(config.workloadPath);
        } catch (WorkloadFormatException e) {
            System.err.println("ไฟล์ workload ผิดรูปแบบ — " + e.getMessage());
            System.exit(1);
            return;
        } catch (java.io.IOException e) {
            System.err.println("เปิดไฟล์ \"" + config.workloadPath + "\" ไม่ได้");
            System.err.println("ตรวจว่าไฟล์มีอยู่จริงและ path ถูกต้อง (สั่ง java จากโฟลเดอร์ใด)");
            System.exit(1);
            return;
        }
        logger.systemStart(config);
        logger.systemEvent("โหลดงานได้ " + jobs.size() + " ชิ้น");

        long startSimulationTime = System.currentTimeMillis();

        // ---------- 3. สร้างส่วนประกอบของระบบ ----------
        // TODO: สร้าง ResourceManager จากจำนวน permit ใน config
        // TODO: สร้าง ReadyQueue ตามนโยบายใน config
        // TODO: สร้าง Statistics
        ResourceManager resourceManager = new ResourceManager(config.printerPermits, config.databasePermits);
        ReadyQueue readyQueue = new ReadyQueue(config.policy);
        Statistics statistics = new Statistics();

        // ช่องทางส่งงานจาก JobGenerator -> Scheduler
        BlockingQueue<Job> arrivalQueue = new ArrayBlockingQueue<>(jobs.size() + 10);

        // ---------- 4. สร้างและเริ่ม Thread ----------
        // TODO: สร้าง Worker จำนวน config.workers ตัว แล้ว start
        // TODO: สร้างและ start Scheduler
        // TODO: สร้างและ start Monitor
        // TODO: สร้างและ start JobGenerator
        //
        List<Worker> workerThreads = new ArrayList<>();
        for (int i = 1; i <= config.workers; i++) {
            Worker worker = new Worker("Worker-" + i, readyQueue, resourceManager, statistics, logger);
            workerThreads.add(worker);
            worker.start();
        }

        Scheduler scheduler = new Scheduler(arrivalQueue, readyQueue, logger);
        scheduler.start();

        Monitor monitor = new Monitor(readyQueue, resourceManager, statistics, logger);
        monitor.start();

        JobGenerator jobGenerator = new JobGenerator(jobs, arrivalQueue, logger);
        jobGenerator.start();
        // ลำดับการ start มีผลหรือไม่ ให้คิดและอธิบายได้ใน Demo

        // ---------- 5. รอจนงานเสร็จครบ ----------
        // TODO: รอจนกว่างานทั้ง jobs.size() ชิ้นจะเสร็จ

        try {
            jobGenerator.join();
            scheduler.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // บล็อกรออยู่ในลูปจนกว่า Worker จะประมวลผล Job ทั้งหมดครบถ้วน
        while (statistics.completedCount() < jobs.size()) {
            try {
                Thread.sleep(100); // ตรวจสอบความคืบหน้าทุก 100 ms โดยไม่กิน CPU
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        //
        // *** นี่คือจุดที่ยากที่สุดของโครงงานนี้ ***
        // Worker ที่กำลังรออยู่ในคิวไม่มีทางรู้ได้เองว่าจะไม่มีงานเข้ามาอีกแล้ว
        // กลุ่มต้องออกแบบวิธีบอก โดยห้ามใช้การเดาเวลา เช่น sleep(10000)
        //
        // เทคนิคที่ไปหาอ่านต่อได้ (เลือกใช้อันใดอันหนึ่งหรือผสมกันก็ได้):
        //   - poison pill
        //   - CountDownLatch
        //   - ตัวนับงานค้างที่ป้องกันด้วย lock
        //
        // อาการผิดที่ต้องไม่เกิด:
        //   1. main จบแล้วแต่ JVM ไม่ปิด เพราะยังมี Thread ค้างอยู่
        //   2. Worker หยุดก่อนที่งานชิ้นสุดท้ายจะทำเสร็จ
        //   3. permit ค้างเพราะถูก interrupt ระหว่างถือ resource

        // ---------- 6. สั่งหยุดทุก Thread ----------
        // TODO: หยุด Worker ทุกตัว, Scheduler, Monitor และ JobGenerator
        // TODO: join ทุก Thread เพื่อยืนยันว่าหยุดจริงก่อนไปขั้นถัดไป
        monitor.interrupt();
        try {
            monitor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ส่ง Poison Pill ให้ Worker Thread ทุกตัวจบการทำงาน
        for (int i = 0; i < config.workers; i++) {
            readyQueue.add(Job.createPoisonPill());
        }

        // รอให้ Worker Thread ทุกตัวปิดตัวลงอย่างสมบูรณ์
        for (Worker worker : workerThreads) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endSimulationTime = System.currentTimeMillis();
        long totalExecutionTimeMs = endSimulationTime - startSimulationTime;

        // ---------- 7. สรุปผล ----------
        // TODO: หา makespan = เวลาที่งานชิ้นสุดท้ายเสร็จ (ใช้ logger.now())
        // TODO: เรียก statistics.printSummary(jobs, makespanMs)
        // TODO: logger.systemStop(completed, jobs.size())
        
        // 1. หาค่า makespan = เวลาที่งานชิ้นสุดท้ายเสร็จ (ใช้เวลาจาก logger)
        long makespanMs = logger.now(); 

        // 2. พิมพ์สรุปผลสถิติลง Terminal
        statistics.printSummary(jobs, makespanMs);

        // 3. แจ้ง logger ว่าระบบหยุดทำงานพร้อมส่งจำนวนงานที่เสร็จจริงเทียบกับงานทั้งหมด
        logger.systemStop(statistics.completedCount(), jobs.size());
    }
}
