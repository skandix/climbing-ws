package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Browse implements IMetadata {
	private final Collection<Area> areas;
	private Metadata metadata;
	
	public Browse(Collection<Area> areas) {
		this.areas = areas;
	}

	public Collection<Area> getAreas() {
		return areas;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}