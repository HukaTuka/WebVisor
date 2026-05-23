package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BLL.Util.AuditService;

public interface AuditAware {
    void setAudit(AuditService audit);
}