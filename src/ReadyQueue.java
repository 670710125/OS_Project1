import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * คิวงานที่พร้อมถูกหยิบไปทำ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * สิ่งที่คลาสนี้ต้องทำได้:
 *   - เก็บงานที่รอ Worker อยู่
 *   - หยิบงานถัดไปตามนโยบายที่เลือก (FCFS หรือ Priority)
 *   - ถูกเรียกจากหลาย Thread พร้อมกันได้อย่างปลอดภัย
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 *   - หัวข้อ 4: priority = 1 สูงสุด เมื่อเท่ากันต้องมีกติกาตัดสินลำดับ (tie-break)
 *     ที่ตัดสินจากข้อมูลของ Job ไม่ขึ้นกับว่า Thread ใดเข้าถึงคิวก่อน
 *   - หัวข้อ 7: ห้ามวนลูปเช็กแบบกิน CPU (busy waiting) — Worker ที่ไม่มีงานทำ
 *     ต้องถูกพักไว้ ไม่ใช่วนถามซ้ำ ๆ
 *
 * จะออกแบบเป็นคลาสเดียวที่รับนโยบายเข้ามา หรือแยกเป็นสองคลาส
 * หรือใช้โครงสร้างข้อมูลสำเร็จรูปของ Java ก็ได้ ขอให้อธิบายเหตุผลได้ใน Demo
 */
public class ReadyQueue {

    // TODO: เก็บนโยบาย (Config.Policy) และโครงสร้างข้อมูลที่ใช้เก็บงาน
    private final BlockingQueue<Job> queue;
    private final AtomicInteger readyCount = new AtomicInteger(0); 

    public ReadyQueue(Config.Policy policy) {
        // TODO
        if (policy == Config.Policy.FCFS) {
            // ใช้ LinkedBlockingQueue สำหรับ FCFS (FIFO: First-In, First-Out)
            this.queue = new LinkedBlockingQueue<>();
        } else if (policy == Config.Policy.PRIORITY) {
            // กำหนด Comparator สำหรับ Priority และ Tie-breaker
            Comparator<Job> priorityComparator = (j1, j2) -> {
                 // pill อยู่หลังงานจริงทุกชิ้นเสมอ
                if (j1.isPoisonPill() != j2.isPoisonPill()) {
                    return j1.isPoisonPill() ? 1 : -1;
    }
                // 1. เปรียบเทียบ priority (ค่าน้อยกว่า = ความสำคัญสูงกว่า)
                int pComp = Integer.compare(j1.getPriority(), j2.getPriority());
                if (pComp != 0) {
                    return pComp;
                }
                // 2. Tie-break: ถ้า priority เท่ากัน ให้เทียบจาก arrivalMs ที่เข้ามาก่อน
                int aComp = Long.compare(j1.getArrivalMs(), j2.getArrivalMs());
                if (aComp != 0) {
                    return aComp;
                }
                // 3. Tie-break สุดท้าย: ถ้า arrivalMs เท่ากันอีก ให้เทียบจาก Job ID
                return j1.getId().compareTo(j2.getId());
            };

            // ใช้ PriorityBlockingQueue พร้อมกับ Comparator ที่สร้างขึ้น
            this.queue = new PriorityBlockingQueue<>(11, priorityComparator);
        } else {
        throw new IllegalArgumentException("Unsupported policy: " + policy);
        }
    
    }

    /** ใส่งานเข้าคิว เรียกโดย Scheduler Thread */
    public void add(Job job) {
        // TODO
       if (!job.isPoisonPill()) {
            readyCount.incrementAndGet(); // เพิ่มก่อน add ตัวนับจะไม่ติดลบชั่วขณะ
        }
        queue.add(job);
    }

    /**
     * หยิบงานถัดไปตามนโยบาย เรียกโดย Worker Thread
     *
     * ถ้ายังไม่มีงาน ต้องรอโดยไม่กิน CPU
     * ต้องคิดด้วยว่าจะบอก Worker อย่างไรเมื่อไม่มีงานเหลือแล้วและควรหยุดทำงาน
     */
    public Job take() throws InterruptedException {
        // TODO
     Job job = queue.take();
    if (!job.isPoisonPill()) {
        readyCount.decrementAndGet();
    }
    return job;
    }

    /** จำนวนงานที่รออยู่ตอนนี้ ใช้โดย Monitor — ต้องอ่านได้อย่างปลอดภัย */
    public int size() {
        // TODO
        return readyCount.get();
    }
}
