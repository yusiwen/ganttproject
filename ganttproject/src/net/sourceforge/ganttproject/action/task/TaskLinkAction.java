/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.action.task;

import java.util.List;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class TaskLinkAction extends TaskActionBase {
    public TaskLinkAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super("task.link", taskManager, selectionManager, uiFacade, null);
    }

    @Override
    protected String getIconFilePrefix() {
        return "link_";
    }

    @Override
    protected void run(List<Task> selection) throws TaskDependencyException {
        for (int i = 0; i < selection.size() - 1; i++) {
            Task dependant = selection.get(i + 1);
            Task dependee = selection.get(i);
            // FIXME If dependant is a supertask containing dependee, this check fails and the dependency is created!!
            if (getTaskManager().getDependencyCollection().canCreateDependency(dependant, dependee)) {
                getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
            }
        }
        // Update (un)link buttons
        getSelectionManager().fireSelectionChanged();
    }

    @Override
    protected boolean isEnabled(List<Task> selection) {
        if(selection.size() <= 1) {
            return false;
        }
        for (int i = 0; i < selection.size() - 1; i++) {
            Task dependant = selection.get(i + 1);
            Task dependee = selection.get(i);
            // FIXME If dependant is a supertask containing dependee, this check fails!
            if (!getTaskManager().getDependencyCollection().canCreateDependency(dependant, dependee)) {
                // It is not possible to create a dependency
                return false;
            }
        }
        return true;
    }
}