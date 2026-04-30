import { useState } from "react";
import { Calendar, CheckCircle2, Edit, MapPin, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/Card/Card";
import { Badge } from "@/components/common/badge";
import { Input } from "@/components/common/input";
import { useToast } from "@/hooks/use-toast";
import { useDocumentTasks } from "@/hooks/api/task/useDocumentTasks";
import type {
  AddTaskToDocumentRequestDto,
  UpdateExistingTaskRequestDto,
} from "@/api/dto/requestDto";
import type {
  ActionPlanTaskDto,
  GetDocumentResponseDto,
} from "@/api/dto/responseDto";
import { brandStyles } from "@/lib/design/styles";

type Props = {
  document: GetDocumentResponseDto;
};

type TaskFormState = {
  title: string;
  dueDate: string;
  location: string;
};

const emptyTaskForm = (): TaskFormState => ({
  title: "",
  dueDate: "",
  location: "",
});

const formatDateForInput = (value: string | null | undefined): string =>
  value ?? "";

const formatDateLabel = (value: string | null | undefined): string | null => {
  if (!value) return null;

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return null;

  return date.toLocaleDateString();
};

const buildAddPayload = (form: TaskFormState): AddTaskToDocumentRequestDto => ({
  title: form.title.trim(),
  dueDate: form.dueDate || undefined,
  location: form.location.trim() || undefined,
});

const buildUpdatePayload = (
  form: TaskFormState,
): UpdateExistingTaskRequestDto => ({
  title: form.title.trim(),
  dueDate: form.dueDate || null,
  location: form.location.trim() || null,
});

export default function ActionItemsSection({ document }: Props) {
  const { toast } = useToast();

  const tasks = document.actionPlanTasks ?? [];
  const [showAddForm, setShowAddForm] = useState(false);
  const [newTask, setNewTask] = useState<TaskFormState>(emptyTaskForm());
  const [editingTaskId, setEditingTaskId] = useState<string | null>(null);
  const [editTask, setEditTask] = useState<TaskFormState>(emptyTaskForm());

  const {
    addTask,
    updateTask,
    deleteTask,
    isAddingTask,
    isUpdatingTask,
    isDeletingTask,
  } = useDocumentTasks({
    documentId: document.id,
    onError: (error) => {
      toast({
        title: "Action item error",
        description: error.userMessage ?? "Please try again.",
        variant: "destructive",
      });
    },
    onAddSuccess: () => {
      setNewTask(emptyTaskForm());
      setShowAddForm(false);
      toast({
        title: "Task added",
        description: "The new action item was added successfully.",
        variant: "success",
      });
    },
    onUpdateSuccess: () => {
      setEditingTaskId(null);
      setEditTask(emptyTaskForm());
      toast({
        title: "Task updated",
        description: "The action item was updated successfully.",
        variant: "success",
      });
    },
    onDeleteSuccess: () => {
      toast({
        title: "Task deleted",
        description: "The action item was deleted successfully.",
        variant: "success",
      });
    },
  });

  const beginEdit = (task: ActionPlanTaskDto) => {
    setShowAddForm(false);
    setEditingTaskId(task.id);
    setEditTask({
      title: task.title ?? "",
      dueDate: formatDateForInput(task.dueDate),
      location: task.location ?? "",
    });
  };

  const cancelEdit = () => {
    setEditingTaskId(null);
    setEditTask(emptyTaskForm());
  };

  const handleAddTask = () => {
    if (!newTask.title.trim() || isAddingTask) {
      return;
    }

    addTask(buildAddPayload(newTask));
  };

  const handleSaveEdit = () => {
    if (!editingTaskId || !editTask.title.trim() || isUpdatingTask) {
      return;
    }

    updateTask({
      taskId: editingTaskId,
      payload: buildUpdatePayload(editTask),
    });
  };

  const handleToggleCompleted = (task: ActionPlanTaskDto) => {
    if (isUpdatingTask) {
      return;
    }

    updateTask({
      taskId: task.id,
      payload: {
        completed: !task.completed,
      },
    });
  };

  const handleDeleteTask = (task: ActionPlanTaskDto) => {
    if (isDeletingTask) {
      return;
    }

    const confirmed = window.confirm(
      `Delete task "${task.title}"? This cannot be undone.`,
    );

    if (!confirmed) {
      return;
    }

    deleteTask(task.id);
  };

  return (
    <Card className={brandStyles.brandCard}>
      <CardHeader>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className={brandStyles.brandText}>Action Items</CardTitle>
            <p className="mt-1 text-sm text-gray-600">
              {tasks.length > 0
                ? `${tasks.length} task${tasks.length === 1 ? "" : "s"} linked to this document`
                : "No action items yet. You can add one below."}
            </p>
          </div>
          <Button
            onClick={() => {
              setEditingTaskId(null);
              setShowAddForm((current) => !current);
            }}
            className={brandStyles.brandButton}
          >
            <Plus className="mr-2 h-4 w-4" />
            {showAddForm ? "Close" : "Add Task"}
          </Button>
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        {showAddForm && (
          <div className="rounded-lg border border-dashed border-gray-200 bg-gray-50 p-4">
            <div className="grid gap-4 md:grid-cols-3">
              <div className="md:col-span-2">
                <label className="mb-2 block text-sm font-medium text-gray-700">
                  Title
                </label>
                <Input
                  value={newTask.title}
                  onChange={(event) =>
                    setNewTask((current) => ({
                      ...current,
                      title: event.target.value,
                    }))
                  }
                  placeholder="Example: Book a follow-up appointment"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-gray-700">
                  Due date
                </label>
                <Input
                  type="date"
                  value={newTask.dueDate}
                  onChange={(event) =>
                    setNewTask((current) => ({
                      ...current,
                      dueDate: event.target.value,
                    }))
                  }
                />
              </div>
            </div>

            <div className="mt-4">
              <label className="mb-2 block text-sm font-medium text-gray-700">
                Location
              </label>
              <Input
                value={newTask.location}
                onChange={(event) =>
                  setNewTask((current) => ({
                    ...current,
                    location: event.target.value,
                  }))
                }
                placeholder="Optional location"
              />
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
                <Button
                  onClick={handleAddTask}
                  disabled={!newTask.title.trim() || isAddingTask}
                  className={brandStyles.brandButton}
                >
                  {isAddingTask ? "Adding..." : "Create Task"}
                </Button>
              <Button
                variant="outline"
                onClick={() => {
                  setShowAddForm(false);
                  setNewTask(emptyTaskForm());
                }}
                className={brandStyles.brandOutlineButton}
              >
                Cancel
              </Button>
            </div>
          </div>
        )}

        {tasks.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-200 p-6 text-center text-sm text-gray-500">
            The document does not currently have any tasks.
          </div>
        ) : (
          <div className="space-y-4">
            {tasks.map((task) => {
              const dueLabel = formatDateLabel(task.dueDate);
              const isEditing = editingTaskId === task.id;

              return (
                <div
                  key={task.id}
                  className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm"
                >
                  {isEditing ? (
                    <div className="space-y-4">
                      <div className="grid gap-4 md:grid-cols-3">
                        <div className="md:col-span-2">
                          <label className="mb-2 block text-sm font-medium text-gray-700">
                            Title
                          </label>
                          <Input
                            value={editTask.title}
                            onChange={(event) =>
                              setEditTask((current) => ({
                                ...current,
                                title: event.target.value,
                              }))
                            }
                          />
                        </div>

                        <div>
                          <label className="mb-2 block text-sm font-medium text-gray-700">
                            Due date
                          </label>
                          <Input
                            type="date"
                            value={editTask.dueDate}
                            onChange={(event) =>
                              setEditTask((current) => ({
                                ...current,
                                dueDate: event.target.value,
                              }))
                            }
                          />
                        </div>
                      </div>

                      <div>
                        <label className="mb-2 block text-sm font-medium text-gray-700">
                          Location
                        </label>
                        <Input
                          value={editTask.location}
                          onChange={(event) =>
                            setEditTask((current) => ({
                              ...current,
                              location: event.target.value,
                            }))
                          }
                        />
                      </div>

                      <div className="flex flex-wrap gap-2">
                        <Button
                          onClick={handleSaveEdit}
                          disabled={!editTask.title.trim() || isUpdatingTask}
                          className={brandStyles.brandButton}
                        >
                          {isUpdatingTask ? "Saving..." : "Save changes"}
                        </Button>
                        <Button
                          variant="outline"
                          onClick={cancelEdit}
                          className={brandStyles.brandOutlineButton}
                        >
                          Cancel
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div className="flex items-start gap-3">
                          <input
                            type="checkbox"
                            checked={task.completed}
                            onChange={() => handleToggleCompleted(task)}
                            className="mt-1 h-5 w-5 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                          />

                          <div className="space-y-2">
                            <div className="flex items-center gap-2">
                              <h3
                                className={`text-base font-semibold ${task.completed ? "text-gray-400 line-through" : "text-gray-900"}`}
                              >
                                {task.title}
                              </h3>
                              {task.completed && (
                                <Badge variant="secondary" className="bg-green-100 text-green-800">
                                  Completed
                                </Badge>
                              )}
                            </div>

                            <div className="flex flex-wrap gap-2 text-sm text-gray-600">
                              {dueLabel && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-3 py-1">
                                  <Calendar className="h-3.5 w-3.5" />
                                  {dueLabel}
                                </span>
                              )}
                              {task.location && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-3 py-1">
                                  <MapPin className="h-3.5 w-3.5" />
                                  {task.location}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            className={brandStyles.brandOutlineButton}
                            onClick={() => beginEdit(task)}
                          >
                            <Edit className="mr-2 h-4 w-4" />
                            Edit
                          </Button>

                          <Button
                            variant="outline"
                            size="sm"
                            className={brandStyles.dangerOutlineButton}
                            onClick={() => handleDeleteTask(task)}
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Delete
                          </Button>
                        </div>
                      </div>

                      {task.completed && (
                        <div className="flex items-center gap-2 text-sm text-green-700">
                          <CheckCircle2 className="h-4 w-4" />
                          Marked complete
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
