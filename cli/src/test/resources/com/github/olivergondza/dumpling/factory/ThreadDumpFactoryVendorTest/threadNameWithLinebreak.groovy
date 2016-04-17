new Thread("thread" + System.getProperty("line.separator") + "name") {
    void run() {
        pauseHere()
    }
}.start();

pauseHere()
