/**
 * ข้อมูลของงานหนึ่งชิ้น
 *
 * ฟิลด์ทั้งหมดในไฟล์นี้มาจากไฟล์ workload CSV โดยตรง และถูกกำหนดครั้งเดียว
 * ตอนโหลด จึงประกาศเป็น final และปลอดภัยเมื่อหลาย Thread อ่านพร้อมกัน
 *
 * ไฟล์นี้เป็นโค้ดตั้งต้นที่อาจารย์แจก แต่ต่างจากไฟล์อื่นตรงที่
 * นักศึกษา "ต้องแก้" โดยเพิ่มฟิลด์ของตัวเองในส่วน TODO ด้านล่าง
 */
public class Job {

    /** รหัสงาน เช่น J01 — ไม่ซ้ำกันภายในหนึ่งไฟล์ workload */
    public final String id;

    /** เวลาที่งานควรเข้าสู่ระบบ นับจากวินาทีที่โปรแกรมเริ่ม (มิลลิวินาที) */
    public final long arrivalMs;

    /** ระดับความสำคัญ โดย 1 คือสูงสุด ตัวเลขยิ่งมากยิ่งสำคัญน้อย */
    public final int priority;

    /** ระยะเวลาของงานหลัก ก่อนขอใช้ทรัพยากรร่วม (มิลลิวินาที) */
    public final long workMs;

    /** ทรัพยากรร่วมที่ต้องใช้ หรือ NONE ถ้าไม่ต้องใช้ */
    public final ResourceType resource;

    /** ระยะเวลาที่ถือครองทรัพยากร (มิลลิวินาที) เป็น 0 เสมอเมื่อ resource เป็น NONE */
    public final long resourceMs;

    /**
     * ลำดับที่งานนี้ปรากฏในไฟล์ workload เริ่มจาก 0
     * เตรียมไว้ให้เผื่อกลุ่มต้องการใช้ประกอบการตัดสินลำดับเมื่อ priority เท่ากัน
     * จะใช้หรือไม่ใช้ก็ได้ กติกาตัดสินลำดับเป็นสิ่งที่กลุ่มต้องออกแบบเอง
     */
    public final int sequence;

    // =====================================================================
    // TODO (นักศึกษา): เพิ่มฟิลด์สำหรับเก็บค่าที่ใช้วัดผลของงานชิ้นนี้
    // =====================================================================
    
    // เวลาที่เข้าสู่ระบบจริง (ใช้อ้างอิงจาก ProjectLogger.now())
    private volatile long actualArrivalMs;
    // เวลาที่ Worker ดึงไปเริ่มทำจริง
    private volatile long startTime;
    // เวลาที่เริ่มเข้าคิวรอ Resource
    private volatile long resourceWaitStartTime;
    // เวลาที่ acquire Resource สำเร็จ
    private volatile long resourceAcquireTime;
    // เวลาที่งานทำเสร็จสมบูรณ์
    private volatile long completionTime;

    // ตัวแปรพิเศษสำหรับสั่งปิด Worker (Poison Pill)
    private final boolean isPoisonPill;

    public Job(String id, long arrivalMs, int priority, long workMs,
               ResourceType resource, long resourceMs, int sequence) {
        this.id = id;
        this.arrivalMs = arrivalMs;
        this.priority = priority;
        this.workMs = workMs;
        this.resource = resource;
        this.resourceMs = resourceMs;
        this.sequence = sequence;
        this.isPoisonPill = false;
    }
    // ==========================================
    // 🟢 เพิ่ม Constructor ตัวนี้ลงไปใน Job.java
    // ==========================================
    private Job(boolean isPoisonPill) {
        this.id = "POISON_PILL";
        this.arrivalMs = -1;
        this.priority = Integer.MAX_VALUE;
        this.workMs = 0;
        this.resource = ResourceType.NONE;
        this.resourceMs = 0;
        this.sequence = -1;
        this.isPoisonPill = isPoisonPill;
    }

    // =====================================================================
    // TODO (นักศึกษา): เพิ่มฟิลด์สำหรับเก็บค่าที่ใช้วัดผลของงานชิ้นนี้เอง

    /** สร้างวัตถุ Poison Pill สำหรับใช้ส่งสัญญาณหยุดการทำงานของ Worker */
    public static Job createPoisonPill() {
        return new Job(true);
    }

    public boolean isPoisonPill() {
        return isPoisonPill;
    }

    // Getters สำหรับฟิลด์เดิมจาก Workload
    public String getId() { return id; }
    public long getArrivalMs() { return arrivalMs; }
    public int getPriority() { return priority; }
    public long getWorkMs() { return workMs; }
    public ResourceType getResource() { return resource; }
    public long getResourceMs() { return resourceMs; }
    public int getSequence() { return sequence; }

    // Getters & Setters สำหรับฟิลด์บันทึกเวลาจริง (Timestamps)
    public long getActualArrivalMs() { return actualArrivalMs; }
    public void setActualArrivalMs(long actualArrivalMs) { this.actualArrivalMs = actualArrivalMs; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getResourceWaitStartTime() { return resourceWaitStartTime; }
    public void setResourceWaitStartTime(long resourceWaitStartTime) { this.resourceWaitStartTime = resourceWaitStartTime; }

    public long getResourceAcquireTime() { return resourceAcquireTime; }
    public void setResourceAcquireTime(long resourceAcquireTime) { this.resourceAcquireTime = resourceAcquireTime; }

    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    //
    // ค่าที่โครงงานต้องการ (ดูหัวข้อ 8 ของเอกสารโจทย์):
    //   - เวลาที่เข้าสู่ระบบจริง
    //   - เวลาที่เริ่มถูกทำโดย Worker
    //   - เวลาที่ทำเสร็จ
    //   - เวลาที่เริ่มรอ resource และเวลารอ resource รวม
    //
    // สามคำถามที่ต้องตอบให้ได้ก่อนเขียน และจะถูกถามใน Demo:
    //   1. ใช้เวลาจากนาฬิกาตัวไหน (ดู ProjectLogger.now() ซึ่งให้เวลาฐานเดียว
    //      กับที่ปรากฏใน log ทำให้ค่าที่วัดกับ log ตรวจสอบกันได้)
    //   2. ฟิลด์ใดถูกเขียนโดย Thread หนึ่งแล้วอ่านโดยอีก Thread หนึ่ง
    //      และต้องป้องกันอย่างไร
    //   3. ผลที่ได้ต้องสอดคล้องกับสมการตรวจสอบในหัวข้อ 8:
    //      Turnaround = Waiting + workMs + Resource Wait + resourceMs
    // =====================================================================

    @Override
    public String toString() {
        return String.format("%s(priority=%d, work=%dms, %s)",
                id, priority, workMs,
                resource == ResourceType.NONE ? "no resource"
                        : resource + " " + resourceMs + "ms");
    }
}
