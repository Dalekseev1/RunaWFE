package ru.runa.wfe.script.permission;

import java.util.List;
import java.util.Set;

import ru.runa.wfe.commons.CollectionUtil;
import ru.runa.wfe.script.common.ScriptExecutionContext;
import ru.runa.wfe.security.Identifiable;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.user.Executor;

/**
 * Change identifiable permission action type.
 */
public enum ChangePermissionType {
    ADD {

        @Override
        public Set<Permission> updatePermission(ScriptExecutionContext context, Executor executor, Identifiable identifiable,
                                                Set<Permission> changedPermission) {
            List<Permission> ownPermissions = context.getAuthorizationLogic().getIssuedPermissions(context.getUser(), executor,
                identifiable);
            return CollectionUtil.unionSet(changedPermission, ownPermissions);
        }
    },
    REMOVE {

        @Override
        public Set<Permission> updatePermission(ScriptExecutionContext context, Executor executor, Identifiable identifiable,
                                                Set<Permission> changedPermission) {
            List<Permission> ownPermissions = context.getAuthorizationLogic().getIssuedPermissions(context.getUser(), executor,
                identifiable);
            return CollectionUtil.diffSet(ownPermissions, changedPermission);
        }
    },
    SET {

        @Override
        public Set<Permission> updatePermission(ScriptExecutionContext context, Executor executor, Identifiable identifiable,
                                                Set<Permission> changedPermission) {
            return changedPermission;
        }
    };

    public abstract Set<Permission> updatePermission(ScriptExecutionContext context, Executor executor,
                                                     Identifiable identifiable, Set<Permission> changedPermission);
}