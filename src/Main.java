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

                // ---------- 5. รอให้ระบบจบเอง ----------
        // Generator ส่ง pill -> Scheduler ส่งต่อ -> Worker ส่งต่อกันเอง
        // pill อยู่หลังงานจริงทุกชิ้น Worker จึงจบหลังงานสุดท้ายเสร็จเสมอ
        try {
            jobGenerator.join();
            scheduler.join();
            for (Worker worker : workerThreads) {
                worker.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ---------- 6. หยุด Monitor (ไม่ได้รับ pill จึงต้อง interrupt) ----------
        long makespanMs = logger.now();   // อ่านก่อนหยุด Monitor
        monitor.interrupt();
        try {
            monitor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ---------- 7. สรุปผล ----------
        statistics.printSummary(jobs, makespanMs);
        logger.systemStop(statistics.completedCount(), jobs.size());
    }
}

