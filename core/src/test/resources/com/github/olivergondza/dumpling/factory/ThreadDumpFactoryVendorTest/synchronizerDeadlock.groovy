import java.util.concurrent.locks.*;

def lockA = new ReentrantLock();
def lockB = new ReentrantLock();

def other = new Thread("other") {
    def void run() {
        lockA.lock();
        lockB.lock();
    }    
}

lockB.lock();
other.start();
Thread.sleep(10);
lockA.lock();
