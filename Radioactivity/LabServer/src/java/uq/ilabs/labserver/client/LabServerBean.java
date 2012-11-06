/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uq.ilabs.labserver.client;

import java.io.Serializable;
import java.util.logging.Level;
import javax.faces.application.ViewExpiredException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import uq.ilabs.library.lab.utilities.Logfile;
import uq.ilabs.library.labserver.client.Consts;
import uq.ilabs.library.labserver.client.LabServerSession;
import uq.ilabs.library.labserver.client.UserSession;

/**
 *
 * @author uqlpayne
 */
@ManagedBean
@SessionScoped
public class LabServerBean implements Serializable {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private static final String STR_ClassName = LabServerBean.class.getName();
    private static final Level logLevel = Level.FINE;
    /*
     * String constants
     */
    private static final String STR_User_arg = "User: %s";
    private static final String STR_Group_arg = "Group: %s";
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private LabServerSession labServerSession;
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Properties">

    public String getCheckViewExpired() {
        if (labServerSession == null) {
            throw new ViewExpiredException();
        }
        return "";
    }

    public String getTitle() {
        return (this.labServerSession != null) ? this.labServerSession.getTitle() : "";
    }

    public String getVersion() {
        return (this.labServerSession != null) ? this.labServerSession.getVersion() : "";
    }

    public String getContactEmail() {
        return labServerSession.getContactEmail();
    }

    public String getUsername() {
        String username = "";

        if (this.labServerSession != null) {
            UserSession userSession = this.labServerSession.getUserSession();
            if (userSession != null && userSession.getUsername() != null) {
                username = userSession.getUsername();
                if (username.length() > 0) {
                    username = String.format(STR_User_arg, username);
                }
            }
        }

        return username;
    }

    public String getGroupname() {
        String groupname = "";

        if (this.labServerSession != null) {
            UserSession userSession = this.labServerSession.getUserSession();
            if (userSession != null && userSession.getGroupname() != null) {
                groupname = userSession.getGroupname();
                if (groupname.length() > 0) {
                    groupname = String.format(STR_Group_arg, groupname);
                }
            }
        }

        return groupname;
    }

    public boolean isLoggedIn() {
        return (this.labServerSession != null && this.labServerSession.getUserSession() != null);
    }
    //</editor-fold>

    /**
     *
     */
    public LabServerBean() {
        final String methodName = "LabServerBean";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        this.labServerSession = (LabServerSession) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(Consts.STRSSN_LabServer);

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);
    }
}