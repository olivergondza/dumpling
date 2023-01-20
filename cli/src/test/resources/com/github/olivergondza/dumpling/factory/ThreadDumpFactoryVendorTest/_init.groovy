import java.lang.management.ManagementFactory

def pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll(/@.*/, '');

assert pid != null && pid != "": "Failed to get ID of the running process"

def writer = new PrintWriter(D.args[0]);
writer.println(pid);
writer.close();

def pauseHere() {
    def o = new Object();
    synchronized (o) {
        while (true) {
            o.wait();
        }
    }
}

/// End of _init
