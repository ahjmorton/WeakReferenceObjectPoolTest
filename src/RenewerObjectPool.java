
class RenewerObjectPool<T> extends ObjectPool<T>  {
	
	private final Renewer<T> renewer;

	protected RenewerObjectPool(int capacity, Creator<T> creator, Renewer<T> renewer) {
		super(capacity, creator);
		this.renewer = renewer;
	}

	@Override
	protected void renewObject(T object) {
		renewer.renew(object);
		
	}

}
