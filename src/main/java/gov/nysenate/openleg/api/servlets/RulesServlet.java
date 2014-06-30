package gov.nysenate.openleg.api.servlets;

import gov.nysenate.openleg.util.Application;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("serial")
public class RulesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File rulesFile = /** KILLED */ null;//new File(Application.getEnvironment().getStorageDirectory(),"CMS.TEXT");
        String rules = FileUtils.readFileToString(rulesFile);
        request.setAttribute("rules", rules);
        getServletContext().getRequestDispatcher("/views/rules-html.jsp").forward(request, response);
    }
}
