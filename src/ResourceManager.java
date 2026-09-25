import java.util.concurrent.Semaphore;

/**
 * ควบคุมสิทธิ์การใช้ทรัพยากรร่วมของทั้งระบบ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 *   - หัวข้อ 5: ใช้ Semaphore ควบคุม PRINTER และ DATABASE
 *     จำนวน permit มาจาก command line (Config)
 *     ในส่วนบังคับให้สร้าง Semaphore แบบ fair = true
 *   - Worker ทุกตัวต้องใช้ ResourceManager object เดียวกัน
 *   - หัวข้อ 7: permit ต้องไม่สูญหายหรือค้าง แม้เกิด exception
 *     หรือถูก interrupt ระหว่างถือ resource
 *
 * คำถามที่จะถูกถามใน Demo:
 *   - ทำไมต้อง fair = true และถ้าเปลี่ยนเป็น false จะเกิดอะไรขึ้น
 *   - ถ้า Thread ถูก interrupt หลัง acquire สำเร็จแต่ก่อน release
 *     โค้ดของกลุ่มยังคืน permit ได้หรือไม่
 */
public class ResourceManager {

    // TODO: เก็บ Semaphore ของ PRINTER และ DATABASE
    private final Semaphore printerSemaphore;
    private final Semaphore databaseSemaphore;

    private final int maxPrinterPermits;
    private final int maxDatabasePermits;

    public ResourceManager(int printerPermits, int databasePermits) {
        // TODO
       this.maxPrinterPermits = printerPermits;
        this.maxDatabasePermits = databasePermits;

        // กำหนด fair = true เพื่อให้คิวการรอ Semaphore เป็นแบบ FCFS (ใครมารอก่อน ได้ก่อน)
        this.printerSemaphore = new Semaphore(printerPermits, true);
        this.databaseSemaphore = new Semaphore(databasePermits, true);
    }

    /** ขอสิทธิ์ใช้ทรัพยากร จะรอจนกว่าจะได้ */
    public void acquire(ResourceType type) throws InterruptedException {
        // TODO
       if (type == ResourceType.PRINTER) {
            printerSemaphore.acquire();
        } else if (type == ResourceType.DATABASE) {
            databaseSemaphore.acquire();
        }
        // ถ้าเป็น NONE หรือประเภทอื่น จะไม่ทำอะไร
    }

    /** คืนสิทธิ์ใช้ทรัพยากร */
    public void release(ResourceType type) {
        // TODO
        if (type == ResourceType.PRINTER) {
            printerSemaphore.release();
        } else if (type == ResourceType.DATABASE) {
            databaseSemaphore.release();
        }    }

    /**
     * ข้อความสั้น ๆ บอกสถานะการใช้ทรัพยากร สำหรับส่งให้ ProjectLogger.monitor()
     * เช่น "printer=1/1 database=0/2"
     */
    public String status() {
        // TODO
        int printerInUse = maxPrinterPermits - printerSemaphore.availablePermits();
        int dbInUse = maxDatabasePermits - databaseSemaphore.availablePermits();

        return String.format("printer=%d/%d database=%d/%d", 
                printerInUse, maxPrinterPermits, 
                dbInUse, maxDatabasePermits);    
    }
}
