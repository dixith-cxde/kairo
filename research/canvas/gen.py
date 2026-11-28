import os
os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), "project"))
s=open('Main.dc.html').read()
boards=[('Light','expanded','Tasks','light'),('TaskDark','task page','Task page','dark'),('TaskLight','task page','Task page','light'),('RepeatDark','repeat sheet','Repeat','dark'),('RepeatLight','repeat sheet','Repeat','light')]
new=[('Select','multi-select','Multi-select'),('Menu','row menu','Row menu'),('Date','date sheet','Date sheet'),('Overdue','overdue','Reschedule overdue'),('Add','quick add','Quick Add'),
     ('Search','search','Search'),('Empty','empty','Empty states'),('Archived','archived','Archived'),('Trash','trash','Trash'),('Notify','notification','Reminder'),
     ('Voice','voice','Voice add'),('Perm','notifications off','Notifications off'),('Drawer','drawer','Lists drawer'),
     ('NewList','new list','New list'),('EditLists','edit lists','Edit lists'),('Hints','hints','Onboarding hints'),
     ('Clock24','date sheet','24-hour clock'),
     ('Habits','habits','Habits'),('HabitsMap','habits heat map','Habits heat map'),('Habit','habit page','Habit page'),('NewHabit','new habit','New habit'),
     ('HabitsSelect','habits select','Habits multi-select'),('HabitsEmpty','habits empty','No habits'),('EditHabit','edit habit','Edit habit'),
     ('HabitArchived','archived habit','Archived habit'),('HabitsPast','habits past day','Habits, past day'),('HabitNotify','habit reminder','Habit reminder'),
     ('Calendar','calendar','Calendar month'),('CalWeek','calendar week','Calendar week'),('CalDay','calendar day','Calendar day'),('NewEvent','new event','New event'),('CalAccess','calendar no access','Calendar access'),('CalAsk','calendar ask','Calendar permission'),
     ('EventPage','event page','Event page'),('EditEvent','edit event','Edit event'),('CalSelect','calendar select','Calendar multi-select'),('CalDrawer','calendar drawer','Calendar drawer'),('CalSearch','calendar search','Calendar search'),('EventNotify','event reminder','Event reminder'),
     ('Finance','finance','Finance'),('FinAccounts','finance accounts','Finance accounts'),('FinAdd','add expense','Add expense'),('FinTxn','transaction page','Transaction page'),('FinReports','finance reports','Reports'),('FinBills','finance bills','Bills'),('FinImport','ivy import','Ivy Wallet import'),('FinRepSetup','report filters','New report'),
     ('Focus','focus','Focus'),('FocusTimer','focus timer','Focus, timer'),('FocusRange','focus range','Focus, from–to'),('FocusPick','focus pick','Focus on'),('FocusCustom','focus custom','Custom Pomodoro'),('FocusSettings','focus settings','Focus settings'),('FocusRun','focus run','Focus running, ring'),('FocusLiquid','focus liquid','Focus running, liquid'),('FocusMinimal','focus minimal','Focus running, minimal'),('FocusStop','focus stop','Stop or give up'),('FocusDone','focus done','Focus done'),('FocusStats','focus stats','Focus stats'),('FocusSession','focus session','Focus session')]
for f,st,t in new: boards += [(f+'Dark',st,t,'dark'),(f+'Light',st,t,'light')]
def light(x):
    return x.replace('"options":["dark","light"],"default":"dark"','"options":["dark","light"],"default":"light"').replace("this.props.theme ?? 'dark'","this.props.theme ?? 'light'")
for f,st,t,th in boards:
    x=s.replace('"default":"expanded"}','"default":"%s"}'%st).replace("this.props.startState ?? 'expanded'","this.props.startState ?? '%s'"%st)
    x=x.replace('<title>Kairo Tasks, dark</title>','<title>Kairo %s, %s</title>'%(t,th))
    if f.startswith('Clock24'):
        x=x.replace('"options":["12-hour","24-hour"],"default":"12-hour"','"options":["12-hour","24-hour"],"default":"24-hour"').replace("this.props.clock ?? '12-hour'","this.props.clock ?? '24-hour'")
    if th=='light': x=light(x)
    open(f+'.dc.html','w').write(x)
# Widgets: Widgets.dc.html is hand-edited (dark); the light twin is generated
w=open('Widgets.dc.html').read()
open('WidgetsLight.dc.html','w').write(light(w).replace('<title>Kairo Widgets, dark</title>','<title>Kairo Widgets, light</title>'))
hw=open('HabitWidgets.dc.html').read()
open('HabitWidgetsLight.dc.html','w').write(light(hw).replace('<title>Kairo Habit widgets, dark</title>','<title>Kairo Habit widgets, light</title>'))
print('generated',len(boards)+1)
