import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ObjectPool<T> {
	private static enum IS {
		PRESENT
	};

	private final int capacity;
	private final ReferenceQueue<T> gcCallback;
	private final Creator<T> creator;
	private final ConcurrentHashMap<Reference<T>, IS> refHolder;

	private final LinkedBlockingQueue<T> objectQueue;

	protected ObjectPool(int capacity, Creator<T> creator) {
		this.creator = creator;
		this.capacity = capacity;
		this.gcCallback = new ReferenceQueue<T>();
		this.objectQueue = new LinkedBlockingQueue<T>(capacity);
		this.refHolder = new ConcurrentHashMap<Reference<T>, IS>(capacity);
	}

	private static final Object LOCK = new Object();

	public T get() throws InterruptedException {
		T instance = null;
		synchronized (LOCK) {
			if (refHolder.size() < capacity) {
				System.out.println(Thread.currentThread().getName()
						+ " creating a new one");
				instance = creator.createNew();
				final Reference<T> ref = new WeakReference<T>(instance,
						gcCallback);
				refHolder.put(ref, IS.PRESENT);
			}
		}

		if (instance == null) {
			System.out.println(Thread.currentThread().getName()
					+ " blocking to retrieve");
			instance = objectQueue.take();
			System.out.println(Thread.currentThread().getName() + " retrieved");

		}

		return instance;
	}

	public void putBack(T instance) {
		renewObject(instance);
		objectQueue.offer(instance);
	}

	protected abstract void renewObject(T object);

	private static void checkArgs(int capacity, Creator<?> creator) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capcity must be greater than 1");
		}
		if (creator == null) {
			throw new IllegalArgumentException("Creator cannot be null");
		}
	}

	private volatile boolean isRunning;
	private Thread gcCallbackProcessor;

	private void init() {
		isRunning = true;
		gcCallbackProcessor = new Thread(new Runnable() {

			@Override
			public void run() {
				while (isRunning) {
					try {
						listen();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

			}

			private void listen() throws InterruptedException {

				final Reference<?> ref = gcCallback.poll();
				if (ref != null) {
					System.out.println(ref + " is being reclaimed");
					refHolder.remove(ref);
					System.out.println("Ref holder looks like " + refHolder);
					final T newInstance = creator.createNew();
					if (objectQueue.offer(newInstance)) {
						final Reference<T> newRef = new WeakReference<T>(
								newInstance, gcCallback);
						refHolder.put(newRef, IS.PRESENT);
					}
				}
			}

		}, "Object pool GC reclaim");
		gcCallbackProcessor.start();
	}

	public void shutDown() {
		isRunning = false;
		try {
			gcCallbackProcessor.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refHolder.clear();
	}

	public static <T> ObjectPool<T> createPool(int capacity,
			Creator<T> creator, Renewer<T> renewer) {
		checkArgs(capacity, creator);
		if (renewer == null) {
			throw new IllegalArgumentException("Renewer cannot be null");
		}
		final ObjectPool<T> pool = new RenewerObjectPool<T>(capacity, creator,
				renewer);
		pool.init();
		return pool;
	}

	public static <T extends Renewable> ObjectPool<T> createPool(int capacity,
			Creator<T> creator) {
		checkArgs(capacity, creator);
		final ObjectPool<T> pool = new RenewableObjectPool<T>(capacity, creator);
		pool.init();
		return pool;
	}

}
