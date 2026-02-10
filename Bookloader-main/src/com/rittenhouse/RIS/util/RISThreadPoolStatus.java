package com.rittenhouse.RIS.util;

/**
 * @author vbhatia
 */
public class RISThreadPoolStatus {

	private boolean started = false;

	private int activeThreads = 0;

	synchronized public void waitDone() {
		try{
			while (activeThreads > 0) {
				wait();
			}
		} catch (InterruptedException e) {}
	}

	synchronized public void waitBegin() {
		try {
			while (!started) {
				wait(1000);
			}
		} catch (InterruptedException e) {
		}
	}

	synchronized public void workerBegin() {
		activeThreads++;
		started = true;
		notify();
	}

	synchronized public void workerEnd() {
		activeThreads--;
		notify();
	}

	synchronized public void reset() {
		activeThreads = 0;
	}
}