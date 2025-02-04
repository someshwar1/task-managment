import { useState } from 'react'
import { BrowserRouter as Router, Route, Routes, Link } from 'react-router-dom'
import { MaterialReactTable, type MRT_ColumnDef } from 'material-react-table'
import { useMemo } from 'react'
import { Button, Dialog, DialogTitle, DialogContent, List, ListItem, ListItemButton, ListItemText } from '@mui/material'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import './App.css'
import { PieChart, Pie, Cell, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, BarChart, Bar } from 'recharts';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';

interface Task {
  id: string;
  title: string;
  description: string;
}

interface Column {
  id: string;
  title: string;
  tasks: Task[];
}


function KanbanBoard() {
  
  const [columns, setColumns] = useState<Column[]>([
    {
      id: 'todo',
      title: 'To Do',
      tasks: [
        { id: '1', title: 'Task 1', description: 'Description 1' },
        { id: '2', title: 'Task 2', description: 'Description 2' }
      ]
    },
    {
      id: 'inProgress',
      title: 'In Progress',
      tasks: [
        { id: '3', title: 'Task 3', description: 'Description 3' }
      ]
    },
    {
      id: 'done',
      title: 'Done',
      tasks: [
        { id: '4', title: 'Task 4', description: 'Description 4' }
      ]
    }
  ]);

  const onDragEnd = (result) => {
    if (!result.destination) return;
  
    const { source, destination } = result;
    const sourceColumn = columns.find(col => col.id === source.droppableId);
    const destColumn = columns.find(col => col.id === destination.droppableId);
    
    if (!sourceColumn || !destColumn) return;
  
    const sourceTasks = [...sourceColumn.tasks];
    const destTasks = sourceColumn === destColumn ? sourceTasks : [...destColumn.tasks];
    const [removed] = sourceTasks.splice(source.index, 1);
    destTasks.splice(destination.index, 0, removed);
  
    const newColumns = columns.map(col => {
      if (col.id === source.droppableId) {
        return {
          ...col,
          tasks: sourceTasks,
        };
      }
      if (col.id === destination.droppableId) {
        return {
          ...col,
          tasks: destTasks,
        };
      }
      return col;
    });
  
    setColumns(newColumns);
  };

  return (
    <div className="kanban-board">
      <h2>Kanban Board</h2>
      <DragDropContext onDragEnd={onDragEnd}>
        <div className="kanban-columns">
          {columns.map(column => (
            <div key={column.id} className="kanban-column">
              <h3>{column.title}</h3>
              <Droppable droppableId={column.id}>
                {(provided) => (
                  <div
                    {...provided.droppableProps}
                    ref={provided.innerRef}
                    className="task-list"
                  >
                    {column.tasks.map((task, index) => (
                      <Draggable
                        key={task.id}
                        draggableId={task.id}
                        index={index}
                      >
                        {(provided) => (
                          <div
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            className="task-card"
                          >
                            <h4>{task.title}</h4>
                            <p>{task.description}</p>
                          </div>
                        )}
                      </Draggable>
                    ))}
                    {provided.placeholder}
                  </div>
                )}
              </Droppable>
            </div>
          ))}
        </div>
      </DragDropContext>
    </div>
  );

}


function AssignUserDialog({ open, onClose, onAssign }) {
  const users = [
    { id: 1, name: 'John Doe' },
    { id: 2, name: 'Jane Smith' },
    { id: 3, name: 'Mike Johnson' }
  ]
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>Assign User to Task</DialogTitle>
      <DialogContent>
        <List>
          {users.map((user) => (
            <ListItem key={user.id} disablePadding>
              <ListItemButton onClick={() => onAssign(user)}>
                <ListItemText primary={user.name} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </DialogContent>
    </Dialog>
  )
}

function Dashboard() {

  const teamData = [
    { id: 1, name: 'John Doe', tasksAssigned: 4 },
    { id: 2, name: 'Jane Smith', tasksAssigned: 6 },
    { id: 3, name: 'Mike Johnson', tasksAssigned: 3 },
    { id: 4, name: 'Sarah Wilson', tasksAssigned: 5 }
  ];

  const pieData = [
    { name: 'Completed', value: 5 },
    { name: 'In Progress', value: 7 },
    { name: 'Pending', value: 8 }
  ];

  const lineData = [
    { name: 'Jan', completed: 4, inProgress: 6 },
    { name: 'Feb', completed: 5, inProgress: 7 },
    { name: 'Mar', completed: 7, inProgress: 5 }
  ];

  const barData = [
    { name: 'John', tasks: 4 },
    { name: 'Jane', tasks: 6 },
    { name: 'Mike', tasks: 3 }
  ];

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28'];
  return (
    <div className="dashboard">
      <h2>Dashboard</h2>
      {/* Stats Cards */}
      <div className="dashboard-grid">
        <div className="dashboard-card">
          <h3>Total Tasks</h3>
          <p>20</p>
        </div>
        <div className="dashboard-card">
          <h3>Completed</h3>
          <p>5</p>
        </div>
        <div className="dashboard-card">
          <h3>In Progress</h3>
          <p>7</p>
        </div>
        <div className="dashboard-card">
          <h3>Pending for Assignment</h3>
          <p>8</p>
        </div>
      </div>
{/* Charts Section */}
<div className="dashboard-charts">
<div className="chart-container">
          <h3>Task Status Distribution</h3>
          <PieChart width={300} height={300}>
            <Pie
              data={pieData}
              cx={150}
              cy={150}
              innerRadius={60}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {pieData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </div>

        <div className="chart-container">
          <h3>Task Progress Trend</h3>
          <LineChart width={500} height={300} data={lineData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="completed" stroke="#0088FE" />
            <Line type="monotone" dataKey="inProgress" stroke="#00C49F" />
          </LineChart>
        </div>
        <div className="chart-container">
          <h3>Task Assignment Distribution</h3>
          <BarChart width={500} height={300} data={barData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="tasks" fill="#8884d8" />
          </BarChart>
        </div>
  
  <div className="team-table-container">
        <h3>Team Task Distribution</h3>
        <TableContainer component={Paper}>
          <Table sx={{ minWidth: 400 }} aria-label="team tasks table">
            <TableHead>
              <TableRow>
                <TableCell>Team Member</TableCell>
                <TableCell align="right">Tasks Assigned</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {teamData.map((member) => (
                <TableRow
                  key={member.id}
                  sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                >
                  <TableCell component="th" scope="row">
                    {member.name}
                  </TableCell>
                  <TableCell align="right">{member.tasksAssigned}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </div>
    </div>
    </div>
  )
}

interface Task {
  taskId: string;
  description: string;
  status: string;
  assigned: string;
}

function TaskDetails() {

  const [openAssignDialog, setOpenAssignDialog] = useState(false)
  const [selectedTask, setSelectedTask] = useState(null)

  const handleAssignUser = (user) => {
    console.log(`Assigning ${user.name} to task ${selectedTask}`)
    setOpenAssignDialog(false)
  }

  const data: Task[] = [
    {
      taskId: 'TASK-001',
      description: 'Create login page',
      status: 'In Progress',
      assigned: 'John Doe',
    },
    {
      taskId: 'TASK-002',
      description: 'Implement dashboard',
      status: 'Completed',
      assigned: 'Jane Smith',
    },
    {
      taskId: 'TASK-003',
      description: 'Fix navigation bug',
      status: 'Pending',
      assigned: 'Mike Johnson',
    },
    {
      taskId: 'TASK-004',
      description: 'Create login page',
      status: 'In Progress',
      assigned: '',
    },
  ]
  const columns = useMemo<MRT_ColumnDef<Task>[]>(
    () => [
      {
        accessorKey: 'taskId',
        header: 'Task ID',
      },
      {
        accessorKey: 'description',
        header: 'Description',
      },
      {
        accessorKey: 'status',
        header: 'Status',
      },
      {
        accessorKey: 'assigned',
        header: 'Assigned To',
      },
      {
        id: 'actions',
        header: 'Actions',
        Cell: ({ row }) => (
          <Button
            variant="contained"
            startIcon={<PersonAddIcon />}
            onClick={() => {
              setSelectedTask(row.original.taskId)
              setOpenAssignDialog(true)
            }}
            size="small"
          >
            Assign
          </Button>
        ),
      },
    ],
    [],
  )
  return (
    <div className="task-details">
      <h2>Task Details</h2>
      <MaterialReactTable 
        columns={columns} 
        data={data}
        enableColumnFilters
        enableSorting
        enablePagination
      />
      <AssignUserDialog
        open={openAssignDialog}
        onClose={() => setOpenAssignDialog(false)}
        onAssign={handleAssignUser}
      />
    </div>
  )
}

function App() {
  return (
    <Router>
      <div className="app-container">
        <nav className="side-nav">
          <ul>
            <li><Link to="/dashboard">Dashboard</Link></li>
            <li><Link to="/tasks">Tasks</Link></li>
            <li><Link to="/kanban">Kanban</Link></li>
          </ul>
        </nav>
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/tasks" element={<TaskDetails />} />
            <Route path='/kanban' element={<KanbanBoard/>} />
          </Routes>
        </main>
      </div>
    </Router>
  )

}

export default App