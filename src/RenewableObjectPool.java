
class RenewableObjectPool<T extends Renewable> extends ObjectPool<T> {

	protected RenewableObjectPool(int capacity, Creator<T> creator) {
		super(capacity, creator);
	}

	@Override
	protected void renewObject(T object) {
		object.renew();
	}
	

}
