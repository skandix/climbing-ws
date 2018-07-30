package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Browse implements IMetadata {
	private final Collection<Area> areas;
	private final LatLng defaultCenter;
	private final int defaultZoom;
	private Metadata metadata;
	
	public Browse(Collection<Area> areas, LatLng defaultCenter, int defaultZoom) {
		this.areas = areas;
		this.defaultCenter = defaultCenter;
		this.defaultZoom = defaultZoom;
	}

	public Collection<Area> getAreas() {
		return areas;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
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