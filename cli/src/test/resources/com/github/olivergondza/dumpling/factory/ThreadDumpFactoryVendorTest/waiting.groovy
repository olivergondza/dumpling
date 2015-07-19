def lock = new Object();
synchronized (lock) {
    lock.wait();
}
