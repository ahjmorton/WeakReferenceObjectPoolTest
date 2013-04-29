
public class Test {

	private static final int CAPACITY = 5;
	private static final int THREAD_COUNT = 10;
	private static final double DROP_RATE = 0.33d;
	private static final long MIN_WAIT = 1000;
	private static final long MAX_WAIT = 1100;
	private static final int ITERATIONS = 3;
	private static final boolean DEFAULT_TASK_STATE = true;

	private static class Task {
		private boolean done;
		
		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		}

	}

	private static enum TaskHandler implements Creator<Task>, Renewer<Task> {
		INSTANCE;

		@Override
		public void renew(Task instance) {
			System.out.println("About to renew an instance");
			if(instance.isDone() == DEFAULT_TASK_STATE) {
				throw new IllegalStateException("Can't renew without work being done");
			}
			instance.setDone(DEFAULT_TASK_STATE);

		}

		@Override
		public Task createNew() {
			final Task task = new Task();
			task.setDone(DEFAULT_TASK_STATE);
			return task;
		}

	};

	public static void main(String[] args) throws InterruptedException {
		final ObjectPool<Task> pool = ObjectPool.createPool(CAPACITY,
				TaskHandler.INSTANCE, TaskHandler.INSTANCE);
		final Runnable runner = new Runnable() {

			private long waitTime() {
				return (long) (MIN_WAIT + ((MAX_WAIT - MIN_WAIT) * Math
						.random()));
			}

			@Override
			public void run() {
				for (int i = 0; i < ITERATIONS; ++i) {
					try {
						System.out.println(Thread.currentThread().getName() + " about to start iteration " + i);
						final Task t = pool.get();
						t.setDone(!DEFAULT_TASK_STATE);
						final long waitTime = waitTime();
						System.out.println(Thread.currentThread().getName() + " about to wait for " + waitTime);
						Thread.sleep(waitTime);
						if (Math.random() > DROP_RATE) {
							System.out.println("Putting object back");
							pool.putBack(t);
						}
						else {
							System.out.println("Dropping object on the floor");
						}

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

		};
		final Thread[] threads = new Thread[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; ++i) {
			System.out.println("About to start task number " + i);
			final Thread thread = new Thread(runner, "Task handler thread " + i);
			thread.start();
			threads[i] = thread;
		}

		for (int i = 0; i < THREAD_COUNT; ++i) {
			System.out.println("Joining thread number " + i);
			threads[i].join();
		}
		pool.shutDown();
	}
}
