package com.buldreinfo.jersey.jaxb.model;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLd;

public class Metadata {
	private final String title;
	private final boolean isAuthenticated;
	private final boolean isAdmin;
	private final boolean isSuperAdmin;
	private String description;
	private JsonLd jsonLd;
	private int defaultZoom;
	private LatLng defaultCenter;
	private boolean isBouldering;
	private List<Grade> grades;
	private List<Type> types;
	
	public Metadata(DbConnection c, Setup setup, String subTitle, String token) {
		this.title = setup.getTitle(subTitle);
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		try {
			Permission p = c.getBuldreinfoRepo().getPermission(token, null, null);
			if (p != null) {
				isAuthenticated = true;
				isAdmin = p.getAdminRegionIds().contains(setup.getIdRegion());
				isSuperAdmin = p.getSuperAdminRegionIds().contains(setup.getIdRegion());
			}
		} catch (NoSuchAlgorithmException | SQLException e) {
			// OK
		}
		this.isAuthenticated = isAuthenticated;
		this.isAdmin = isAdmin;
		this.isSuperAdmin = isSuperAdmin;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public JsonLd getJsonLd() {
		return jsonLd;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Type> getTypes() {
		return types;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	public boolean isAuthenticated() {
		return isAuthenticated;
	}
	
	public boolean isBouldering() {
		return isBouldering;
	}

	public boolean isSuperAdmin() {
		return isSuperAdmin;
	}

	public Metadata setDefaultCenter(LatLng defaultCenter) {
		this.defaultCenter = defaultCenter;
		return this;
	}

	public Metadata setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Metadata setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public Metadata setGrades(List<Grade> grades) {
		this.grades = grades;
		return this;
	}

	public Metadata setIsBouldering(boolean isBouldering) {
		this.isBouldering = isBouldering;
		return this;
	}
	
	public Metadata setJsonLd(JsonLd jsonLd) {
		this.jsonLd = jsonLd;
		return this;
	}

	public Metadata setTypes(List<Type> types) {
		this.types = types;
		return this;
	}
}