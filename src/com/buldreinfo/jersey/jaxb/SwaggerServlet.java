package com.buldreinfo.jersey.jaxb;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import io.swagger.jaxrs.config.BeanConfig;

/**
 * Creates https://brattelinjer.no/com.buldreinfo.jersey.jaxb/swagger.json
 */
@SuppressWarnings("serial")
public class SwaggerServlet extends HttpServlet {
	@Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.0");
        beanConfig.setSchemes(new String[]{"https"});
        beanConfig.setHost("brattelinjer.no");
        beanConfig.setBasePath("/com.buldreinfo.jersey.jaxb");
        beanConfig.setResourcePackage("com.buldreinfo.jersey.jaxb");
        beanConfig.setScan(true);
    }
}
