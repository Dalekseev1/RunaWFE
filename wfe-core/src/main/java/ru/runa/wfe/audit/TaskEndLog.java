/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ru.runa.wfe.audit;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import ru.runa.wfe.audit.presentation.ExecutorNameValue;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.lang.StartNode;
import ru.runa.wfe.lang.Transition;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.task.TaskCompletionInfo;
import ru.runa.wfe.user.Actor;

/**
 * Logging task completion.
 * 
 * @author Dofs
 */
@Entity
@DiscriminatorValue(value = "3")
public class TaskEndLog extends TaskLog {
    private static final long serialVersionUID = 1L;

    public TaskEndLog() {
    }

    public TaskEndLog(Task task, TaskCompletionInfo completionInfo) {
        super(task);
        if (completionInfo.getExecutor() != null) {
            addAttribute(ATTR_ACTOR_NAME, completionInfo.getExecutor().getName());
        }
        addAttribute(ATTR_TRANSITION_NAME, completionInfo.getTransitionName());
    }

    public TaskEndLog(Process process, StartNode startNode, Actor actor, String transitionName) {
        super(process, startNode);
        addAttribute(ATTR_ACTOR_NAME, actor.getName());
        addAttribute(ATTR_TRANSITION_NAME, transitionName);
    }

    @Transient
    public String getTransitionName() {
        return getAttribute(ATTR_TRANSITION_NAME);
    }

    @Transient
    public String getActorName() {
        String actorName = getAttribute(ATTR_ACTOR_NAME);
        if (actorName != null) {
            return actorName;
        }
        return "";
    }

    @Override
    @Transient
    public Object[] getPatternArguments() {
        return new Object[] { getTaskName(), new ExecutorNameValue(getActorName()) };
    }

    @Override
    public void processBy(ProcessLogVisitor visitor) {
        visitor.onTaskEndLog(this);
    }
}
