package net.mine_diver.optiforge.patcher;

import java.util.Objects;

class Lambda {
	public final String owner, name, desc;
	public final String method;

	public Lambda(String owner, String name, String desc, String method) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.method = method;
	}

	public String getFullName() {
		return owner + '#' + name + desc;
	}

	public String getName() {
		return name.concat(desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name, desc, method);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Lambda)) return false;

		Lambda that = (Lambda) obj;
		return Objects.equals(owner, that.owner) && Objects.equals(name, that.name) && Objects.equals(desc, that.desc) && Objects.equals(method, that.method);
	}
}