/**
 * Thread ที่ดึงงานจาก Ready Queue ไปทำจนเสร็จ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ลำดับการทำงานของ Job หนึ่งชิ้น บังคับตามหัวข้อ 6 ของโจทย์:
 *   1. รับงานจาก Ready Queue แล้วบันทึกเวลาเริ่ม
 *   2. จำลองงานหลักด้วย Thread.sleep(job.workMs)
 *   3. ถ้า job.resource != NONE ให้บันทึกเวลาเริ่มรอ แล้ว acquire
 *   4. จำลองการถือครองด้วย Thread.sleep(job.resourceMs)
 *   5. release แล้วบันทึกเวลาจบ
 *
 * ห้ามสลับขั้นที่ 2 กับ 3 เพราะจะทำให้ผลของทุกกลุ่มเทียบกันไม่ได้
 *
 * จุดที่มักพลาด:
 *   - ถ้า exception หรือ interrupt เกิดขึ้นหลัง acquire แต่ก่อน release
 *     permit จะค้างถาวรและระบบจะแขวน ต้องออกแบบให้คืนได้เสมอ
 *   - Worker ต้องหยุดเองได้เมื่อไม่มีงานเหลือแล้ว ไม่ใช่วนรอตลอดไป
 */
public class Worker extends Thread {

    // TODO: เก็บ ReadyQueue, ResourceManager, Statistics และ logger
    private final ReadyQueue readyQueue;
    private final ResourceManager resources;
    private final Statistics statistics;
    private final ProjectLogger logger;

    public Worker(String name, ReadyQueue readyQueue, ResourceManager resources,
                  Statistics statistics, ProjectLogger logger) {
        super(name);
        // TODO
        this.readyQueue = readyQueue;
        this.resources = resources;
        this.statistics = statistics;
        this.logger = logger;   
    }

    @Override
    public void run() {
        // TODO: วนรับงานและเรียก processJob จนกว่าจะได้รับสัญญาณให้หยุด
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // ดึงงานจากคิว (ถ้าไม่มีงานจะถูกบล็อกรอที่ method นี้)
                Job job = readyQueue.take();

                // ตรวจสอบ Poison Pill สำหรับสัญญาณปิดการทำงาน
                if (job.isPoisonPill()) {
                    readyQueue.add(job);   // ส่งต่อให้ Worker ตัวถัดไป 
                    break;
                }

                // แจ้ง Statistics ว่ามีงานกำลังรันเพิ่มขึ้น
                statistics.incrementRunningJobs();

                try {
                    processJob(job);
                     statistics.incrementCompletedJobs();
                } finally {
                    // เมื่องานเสร็จ (หรือเกิดข้อผิดพลาด) ปรับลดจำนวนงานกำลังรัน และเพิ่มจำนวนงานที่เสร็จแล้ว
                    statistics.decrementRunningJobs();
                   
                }
            }
        } catch (InterruptedException e) {
            // Thread ถูก interrupt เพื่อให้หยุดการทำงาน
            Thread.currentThread().interrupt();
        }
    }

    /** ทำงานหนึ่งชิ้นให้จบตามลำดับ 5 ขั้นด้านบน */
    private void processJob(Job job) throws InterruptedException {
        // TODO
        //long now = System.currentTimeMillis();

        // 1. บันทึกเวลาเริ่มงาน
        job.setStartTime(logger.now());
        logger.jobStarted(job);

        // 2. จำลองงานหลักด้วย Thread.sleep(workMs)
        if (job.getWorkMs() > 0) {
            Thread.sleep(job.getWorkMs());
        }

        // 3. จัดการ Resource ร่วม (ถ้ามี)
        if (job.getResource() != ResourceType.NONE) {
            // บันทึกเวลาเริ่มรอ Resource
            long waitStart = logger.now();
            job.setResourceWaitStartTime(waitStart);
            logger.resourceWaitStarted(job);

            // ขอใช้ Resource
            resources.acquire(job.getResource());
            
            
            try {
            long acquireTime = logger.now();
            job.setResourceAcquireTime(acquireTime);
            long waitedMs = acquireTime - waitStart;
    
            logger.resourceAcquired(job, waitedMs);

                if (job.getResourceMs() > 0) {
                    Thread.sleep(job.getResourceMs());
                }
            } finally {
                resources.release(job.getResource());
                logger.resourceReleased(job);
            }
        }

        // บันทึกเวลาที่งานเสร็จสมบูรณ์
         job.setCompletionTime(logger.now());
        logger.jobCompleted(job);

        // เพิ่มข้อมูล Job นี้ลงใน Statistics รวม
        statistics.addCompletedJob(job);
    }
}
