def lock = new Object();
synchronized (lock) {
    lock.wait(1000000);
}
