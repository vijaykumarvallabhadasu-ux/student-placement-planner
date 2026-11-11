<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, java.sql.Date, java.time.LocalDate" %>
<%@ include file="_header.jspf" %>

<%
    List<Map<String, Object>> tasks =
            (List<Map<String, Object>>) request.getAttribute("tasks");
    int taskTotal = tasks != null ? tasks.size() : 0;
    LocalDate today = LocalDate.now();
%>

<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">ACTION PLAN &amp; MILESTONES</span>
        <h1>Placement Preparation Tasks</h1>
        <p>Set daily coding milestones, schedule mock interview practice, and track completion progress leading up to campus drives.</p>
    </div>
</section>

<% if ("1".equals(request.getParameter("added"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Preparation milestone scheduled successfully!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("updated"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Task status updated in Oracle Database!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("deleted"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Task deleted successfully.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("duplicate"))) { %>
    <div class="alert info" role="status">
        <span>&#8505; This task is already scheduled in your list. Duplicate tasks are prevented.</span>
    </div>
<% } %>

<% if ("emptytitle".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Task title is required and cannot be empty.</span>
    </div>
<% } %>

<% if ("dbfailed".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Database operation could not be completed. Please try again.</span>
    </div>
<% } %>

<% if ("invalidstatus".equals(request.getParameter("error")) || "invalidid".equals(request.getParameter("error")) || "missingid".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Invalid task request. Please try again.</span>
    </div>
<% } %>

<% if (request.getAttribute("dbError") != null) { %>
    <div class="alert error" role="alert">
        <span>&#9888; <%= request.getAttribute("dbError") %></span>
    </div>
<% } %>

<!-- Add Task Panel -->
<section class="panel">
    <div class="panel-title">
        <span>Schedule a New Preparation Milestone</span>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/tasks">
        <div style="display: grid; grid-template-columns: 2fr 1fr auto; gap: 14px; align-items: flex-end;">
            <div class="form-group" style="margin-bottom: 0;">
                <label for="title">Task Title *</label>
                <input
                    type="text"
                    id="title"
                    name="title"
                    placeholder="e.g. Solve 5 Dynamic Programming problems, Practice SQL Joins..."
                    maxlength="200"
                    required>
            </div>

            <div class="form-group" style="margin-bottom: 0;">
                <label for="dueDate">Target Due Date</label>
                <input
                    type="date"
                    id="dueDate"
                    name="dueDate">
            </div>

            <button type="submit" class="btn primary" style="height: 44px;">
                + Add Task
            </button>
        </div>
    </form>
</section>

<!-- Tasks List Panel -->
<section class="panel">
    <div class="panel-title">
        <span>My Preparation Milestones (<%= taskTotal %>)</span>
        <div style="display: flex; gap: 8px;">
            <button type="button" class="btn sm secondary task-filter-btn active" data-filter="all">All</button>
            <button type="button" class="btn sm secondary task-filter-btn" data-filter="pending">Pending</button>
            <button type="button" class="btn sm secondary task-filter-btn" data-filter="completed">Completed</button>
            <button type="button" class="btn sm secondary task-filter-btn" data-filter="overdue">Overdue</button>
        </div>
    </div>

    <% if (tasks == null || tasks.isEmpty()) { %>
        <div class="empty-state">
            <div class="empty-icon">&#128221;</div>
            <h3>No preparation tasks scheduled</h3>
            <p>Break down your campus placement goals into manageable milestones using the form above.</p>
        </div>
    <% } else { %>
        <div class="task-list">
            <%
                for (Map<String, Object> task : tasks) {
                    boolean isDone = "Completed".equalsIgnoreCase((String) task.get("status"));
                    Object dueDateObj = task.get("dueDate");
                    if (dueDateObj == null) {
                        dueDateObj = task.get("due_date");
                    }
                    Date dueDate = null;
                    LocalDate dDate = null;
                    if (dueDateObj instanceof java.sql.Date) {
                        dueDate = (java.sql.Date) dueDateObj;
                        dDate = dueDate.toLocalDate();
                    } else if (dueDateObj instanceof java.sql.Timestamp) {
                        dueDate = new java.sql.Date(((java.sql.Timestamp) dueDateObj).getTime());
                        dDate = dueDate.toLocalDate();
                    } else if (dueDateObj instanceof java.util.Date) {
                        dueDate = new java.sql.Date(((java.util.Date) dueDateObj).getTime());
                        dDate = dueDate.toLocalDate();
                    }

                    boolean isOverdue = false;
                    String dueLabel = "Flexible";

                    if (dDate != null) {
                        dueLabel = dDate.toString();
                        if (!isDone && dDate.isBefore(today)) {
                            isOverdue = true;
                        } else if (dDate.isEqual(today)) {
                            dueLabel = "Due Today (" + dDate + ")";
                        }
                    }

                    long taskId = 0L;
                    Object idObj = task.get("id");
                    if (idObj == null) idObj = task.get("task_id");
                    if (idObj instanceof Number) {
                        taskId = ((Number) idObj).longValue();
                    } else if (idObj != null) {
                        try { taskId = Long.parseLong(idObj.toString()); } catch (NumberFormatException ignored) {}
                    }

                    String rawTitle = task.get("title") != null ? task.get("title").toString()
                            : (task.get("task_title") != null ? task.get("task_title").toString() : "Milestone");
                    String safeTitle = rawTitle.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
            %>
                <div class="task-item <%= isDone ? "completed" : "" %> <%= isOverdue ? "overdue" : "" %>">
                    <div class="task-left">
                        <form method="post" action="${pageContext.request.contextPath}/tasks" style="display:inline;">
                            <input type="hidden" name="action" value="status">
                            <input type="hidden" name="id" value="<%= taskId %>">
                            <input type="hidden" name="status" value="<%= isDone ? "Pending" : "Completed" %>">
                            <button
                                type="submit"
                                class="btn sm <%= isDone ? "secondary" : "primary" %>"
                                title="<%= isDone ? "Mark as Pending" : "Mark as Completed" %>">
                                <%= isDone ? "&#10003; Done" : "Mark Done" %>
                            </button>
                        </form>

                        <div>
                            <div class="task-title" id="title-text-<%= taskId %>"><%= safeTitle %></div>
                            <div class="task-meta">
                                Target Date: <span id="date-text-<%= taskId %>"><%= dueLabel %></span>
                                <% if (isOverdue) { %>
                                    <span class="badge" style="background: var(--danger-light); color: var(--danger); font-size: 0.74rem; margin-left: 6px;">
                                        &#9888; Overdue
                                    </span>
                                <% } %>
                            </div>

                            <!-- Inline Edit Task Form (Hidden by default) -->
                            <form method="post" action="${pageContext.request.contextPath}/tasks" id="edit-form-<%= taskId %>" style="display: none; margin-top: 10px; gap: 8px;">
                                <input type="hidden" name="action" value="edit">
                                <input type="hidden" name="id" value="<%= taskId %>">
                                <input type="text" name="title" value="<%= safeTitle %>" required style="padding: 6px 10px; font-size: 0.88rem; width: 260px;">
                                <input type="date" name="dueDate" value="<%= dDate != null ? dDate.toString() : "" %>" style="padding: 6px 10px; font-size: 0.88rem; width: 140px;">
                                <button type="submit" class="btn sm primary">Save</button>
                                <button type="button" class="btn sm secondary" onclick="toggleTaskEdit(<%= taskId %>)">Cancel</button>
                            </form>
                        </div>
                    </div>

                    <div class="task-actions">
                        <span class="badge <%= isDone ? "badge-completed" : "badge-pending" %>">
                            <%= isDone ? "Completed" : "Pending" %>
                        </span>

                        <button type="button" class="btn sm secondary" onclick="toggleTaskEdit(<%= taskId %>)" title="Edit task details">
                            &#9998;
                        </button>

                        <form method="post" action="${pageContext.request.contextPath}/tasks" data-confirm="Are you sure you want to delete this milestone?">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="<%= taskId %>">
                            <button type="submit" class="btn sm outline-danger" title="Delete milestone">
                                &#128465;
                            </button>
                        </form>
                    </div>
                </div>
            <% } %>
        </div>
    <% } %>
</section>

<script>
function toggleTaskEdit(taskId) {
    var form = document.getElementById('edit-form-' + taskId);
    if (!form) return;
    if (form.style.display === 'none' || form.style.display === '') {
        form.style.display = 'flex';
    } else {
        form.style.display = 'none';
    }
}
</script>

<%@ include file="_footer.jspf" %>
