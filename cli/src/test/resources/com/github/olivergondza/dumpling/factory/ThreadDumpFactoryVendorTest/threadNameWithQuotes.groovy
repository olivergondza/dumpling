new Thread("a\"thread\"") {
    void run() {
        pauseHere()
    }
}.start()

pauseHere()
