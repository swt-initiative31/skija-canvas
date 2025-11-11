package org.eclipse.swt.graphics;

import java.util.*;

public final class RegionLog {

	enum OpType {
		ADD, INTERSECT, SUBTRACT, TRANSLATE
	}

	record Operation(OpType type, Object executionObject) {
	}

	private final List<Operation> op = new LinkedList<>();


	public void translate(Point p) {
		op.add(new Operation(OpType.TRANSLATE, p));
	}

	public void intersect(Object ob) {
		op.add(new Operation(OpType.INTERSECT, ob));
	}

	public void add(Object ob) {
		op.add(new Operation(OpType.ADD, ob));
	}

	public void subtract(Object ob) {
		op.add(new Operation(OpType.SUBTRACT, ob));
	}

	List<Operation> getOperations(){
		return op;
	}
}
