import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * ปล่อยงานเข้าสู่ระบบตามเวลา arrivalMs ของแต่ละ Job
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * หน้าที่ (หัวข้อ 3 ของโจทย์):
 *   - รอจนถึงเวลา arrivalMs ของแต่ละงาน แล้วส่งงานต่อไปยัง Scheduler
 *   - บันทึกเวลาที่งานเข้าสู่ระบบ "จริง" ลงใน Job
 *     (อาจไม่ตรงกับ arrivalMs เป๊ะ เพราะ Thread ถูกปลุกช้าได้)
 *   - เรียก logger.jobArrived(job) ทุกครั้งที่ปล่อยงาน
 *
 * ข้อควรคิด:
 *   - รายการงานที่ได้จาก WorkloadLoader เรียงตามลำดับในไฟล์ ไม่ได้เรียงตามเวลา
 *   - เมื่อปล่อยงานครบทุกชิ้นแล้ว ต้องมีวิธีบอกระบบว่า "จะไม่มีงานเข้ามาอีก"
 *     ดู TODO เรื่องการปิดระบบใน Main
 */
public class JobGenerator extends Thread {

    // TODO: เก็บรายการงาน, ช่องทางส่งงานไปยัง Scheduler และ logger
    private final List<Job> jobs;
    private final BlockingQueue<Job> arrivalQueue;
    private final ProjectLogger logger;
    //
    // หมายเหตุ: constructor ด้านล่างยังไม่มี parameter สำหรับ "ช่องทางส่งงาน"
    // เพราะเป็นสิ่งที่กลุ่มต้องออกแบบเอง (หัวข้อ 2 ห้ามให้ JobGenerator
    // ใส่งานลง ReadyQueue โดยตรง ต้องผ่าน Scheduler เสมอ)
    // ให้เพิ่ม parameter เข้าไปตามที่ออกแบบ เช่น BlockingQueue<Job>
    // หรือคลาสของกลุ่มเอง — เพิ่ม parameter ได้ แต่อย่าเปลี่ยนชื่อคลาส

    public JobGenerator(List<Job> jobs, BlockingQueue<Job> arrivalQueue, ProjectLogger logger) {
        super("generator");
        // ก๊อปปี้รายการ Job มาเพื่อไม่ให้กระทบ List เดิม
        this.jobs = new ArrayList<>(jobs);
        this.arrivalQueue = arrivalQueue;
        this.logger = logger;
    }

    @Override
    public void run() {
     try {
            jobs.sort(Comparator.comparingLong(Job::getArrivalMs));

            for (Job job : jobs) {
                long elapsedTime = logger.now();
                long sleepTime = job.getArrivalMs() - elapsedTime;

                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }

                // บันทึกเวลาที่เข้าจริง และลง Log
                job.setActualArrivalMs(logger.now());
                logger.jobArrived(job);

                arrivalQueue.put(job);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
            finally {
        arrivalQueue.offer(Job.createPoisonPill()); // ส่งเสมอ ไม่ว่าจบแบบไหน
    }
    }
}
