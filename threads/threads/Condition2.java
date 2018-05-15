package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param conditionLock
	 *            the lock associated with this condition variable. The current
	 *            thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 *            <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;

	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		//disable interrupt
		boolean intStatus = Machine.interrupt().disable();
		
		conditionLock.release();

		//current thread is waiting for access
		waitQueue.waitForAccess(KThread.currentThread());
		queueSize += 1;

		//put thread to sleep
		KThread.sleep();

		conditionLock.acquire();

		//enable interrupt
		Machine.interrupt().restore(intStatus);

	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		//disable interrupt
		boolean intStatus = Machine.interrupt().disable();

		//wakes up one thread at a time.
		KThread nt = waitQueue.nextThread();
		
		//if that thread isn't null
		if (nt != null) { 

			queueSize -= 1;
			//move thread to ready queue
			nt.ready();

		}
		//enable interrupt
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current thread
	 * must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		//while thread and queue aren't null, wake
		while(queueSize > 0)
			wake();
		
	}

	private Lock conditionLock;
	private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	private int queueSize = 0;
}