import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * รวบรวมและคำนวณค่าที่ใช้วัดผลของการรันหนึ่งครั้ง
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้ (หัวข้อ 8):
 *   - Waiting Time, Turnaround Time, Throughput, Resource Wait Time
 *   - ต้องถูกอัปเดตจากหลาย Worker พร้อมกันได้อย่างปลอดภัย
 *   - ผลต้องสอดคล้องกับสมการตรวจสอบ:
 *       Turnaround = Waiting + workMs + Resource Wait + resourceMs
 *     ใช้สมการนี้ตรวจงานทีละชิ้นได้ว่าค่าไหนคำนวณผิด
 *
 * ข้อควรระวัง: ค่าเฉลี่ยของ Resource Wait ให้คิดเฉพาะงานที่ใช้ resource
 * ส่วนงานที่ resource = NONE ให้ถือว่า Resource Wait เป็น 0
 */
public class Statistics {

    // TODO: เก็บข้อมูลของงานที่เสร็จแล้ว หรือเก็บผลรวมไว้คำนวณทีหลัง
        private final AtomicInteger runningJobsCount = new AtomicInteger(0);
        private final AtomicInteger completedJobsCount = new AtomicInteger(0);
        
        // เก็บรายการ Job ที่ทำเสร็จแล้ว โดยใช้ SynchronizedList เพื่อป้องกัน Data Race
        private final List<Job> completedJobsList = Collections.synchronizedList(new ArrayList<>());
   
     /** บันทึกเพิ่มจำนวนงานที่กำลังทำอยู่ (เรียกโดย Worker) */
    public void incrementRunningJobs() {
        runningJobsCount.incrementAndGet();
    }

    /** บันทึกลดจำนวนงานที่กำลังทำอยู่ (เรียกโดย Worker) */
    public void decrementRunningJobs() {
        runningJobsCount.decrementAndGet();
    }

    /** อ่านจำนวนงานที่กำลังทำอยู่ตอนนี้ (ใช้โดย Monitor) */
    public int getRunningJobsCount() {
        return runningJobsCount.get();
    }

    /** เพิ่มจำนวนงานที่เสร็จแล้วขึ้น 1 (เรียกโดย Worker) */
    public void incrementCompletedJobs() {
        completedJobsCount.incrementAndGet();
    }

    /** บันทึกว่างานชิ้นหนึ่งเสร็จแล้ว เรียกโดย Worker หลายตัวพร้อมกันได้ */
    public void recordCompletion(Job job) {
        completedJobsList.add(job);
    }

    // method สำหรับเรียกใช้ร่วมกับ Worker ที่ครอบครอบไว้ก่อนหน้า
    public void addCompletedJob(Job job) {
        recordCompletion(job);
    }

    /** จำนวนงานที่เสร็จแล้ว ใช้โดย Monitor และใช้ตรวจว่างานครบหรือยัง */
    public int completedCount() {
        return completedJobsCount.get();
    }

    /**
     * พิมพ์ตารางสรุปผลตอนจบโปรแกรม
     * อย่างน้อยต้องมี avg Waiting Time, avg Turnaround Time,
     * Throughput และ avg Resource Wait Time
     *
     * ตามหัวข้อ 14 ให้รายงานเวลาเป็นจำนวนเต็มหน่วย ms
     * และ Throughput อย่างน้อย 2 ตำแหน่งทศนิยม
     */
    public void printSummary(List<Job> allJobs, long makespanMs) {
        // TODO
        double totalWT = 0;
        double totalTAT = 0;
        double totalRW = 0;
        int resourceJobCount = 0;

        for (Job job : allJobs) {
            // คำนวณค่าของ Job แต่ละตัว
            long waitingTime = job.getStartTime() - job.getArrivalMs();
            long turnaroundTime = job.getCompletionTime() - job.getArrivalMs();

            long resourceWaitTime = 0;
            if (job.getResource() != ResourceType.NONE) {
                resourceWaitTime = job.getResourceAcquireTime() - job.getResourceWaitStartTime();
                totalRW += resourceWaitTime;
                resourceJobCount++;
            }

            totalWT += waitingTime;
            totalTAT += turnaroundTime;
        }

        int totalJobs = allJobs.size();
        double avgWT = totalJobs > 0 ? totalWT / totalJobs : 0;
        double avgTAT = totalJobs > 0 ? totalTAT / totalJobs : 0;
        double avgRW = resourceJobCount > 0 ? totalRW / resourceJobCount : 0;

        // Throughput = จำนวน Job ทั้งหมด / เวลาประมวลผลรวมในหน่วยวินาที
        double throughput = makespanMs > 0 ? (double) totalJobs / (makespanMs / 1000.0) : 0;

        System.out.println("\n========== SIMULATION SUMMARY ==========");
        System.out.println(String.format("Total Jobs Processed : %d", totalJobs));
        System.out.println(String.format("Total Execution Time : %d ms", makespanMs));
        System.out.println(String.format("Average Waiting Time (WT) : %.0f ms", avgWT));
        System.out.println(String.format("Average Turnaround Time (TAT) : %.0f ms", avgTAT));
        System.out.println(String.format("Average Resource Wait (RW) : %.0f ms", avgRW));
        System.out.println(String.format("Throughput : %.2f jobs/sec", throughput));
        System.out.println("========================================\n");
    }
}
