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
    List<Job> done;
    synchronized (completedJobsList) {
        done = new ArrayList<>(completedJobsList);
    }

    double totalWT = 0, totalTAT = 0, totalRW = 0;
    int resourceJobCount = 0;
    long maxDrift = 0;

    for (Job job : done) {
        long wt  = job.getStartTime() - job.getActualArrivalMs();
        long tat = job.getCompletionTime() - job.getActualArrivalMs();

        long rw = 0;
        if (job.getResource() != ResourceType.NONE) {
            rw = job.getResourceAcquireTime() - job.getResourceWaitStartTime();
            totalRW += rw;
            resourceJobCount++;
        }

        // ตรวจสมการ: TAT = WT + workMs + RW + resourceMs (คลาดได้เล็กน้อยจาก sleep)
        long drift = tat - (wt + job.getWorkMs() + rw + job.getResourceMs());
        maxDrift = Math.max(maxDrift, Math.abs(drift));

        totalWT += wt;
        totalTAT += tat;
    }

    int n = done.size();
    double avgWT  = n > 0 ? totalWT / n : 0;
    double avgTAT = n > 0 ? totalTAT / n : 0;
    double avgRW  = resourceJobCount > 0 ? totalRW / resourceJobCount : 0;
    double throughput = makespanMs > 0 ? n / (makespanMs / 1000.0) : 0;

    System.out.println("\n========== SIMULATION SUMMARY ==========");
    if (n != allJobs.size()) {
        System.out.println("WARNING: completed " + n + " of " + allJobs.size() + " jobs");
    }
    System.out.printf("Total Jobs Completed : %d%n", n);
    System.out.printf("Total Execution Time : %d ms%n", makespanMs);
    System.out.printf("Average Waiting Time (WT)     : %.0f ms%n", avgWT);
    System.out.printf("Average Turnaround Time (TAT) : %.0f ms%n", avgTAT);
    System.out.printf("Average Resource Wait (RW)    : %.0f ms%n", avgRW);
    System.out.printf("Throughput : %.2f jobs/sec%n", throughput);
    System.out.printf("Max per-job equation drift    : %d ms%n", maxDrift);
    System.out.println("========================================\n");
}
}
