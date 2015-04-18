#this script currently is designed for being run on Nexus S
import sys

if (len(sys.argv) < 3):
  print "Usage: monkeyrunner monkey.py {lang} {stage}"
  sys.exit(0)

lang = sys.argv[1]
stage = sys.argv[2]
targetdir = '/home/michael/programmieren/MyExpenses/doc/screenshots/neu/' + lang + '/'
BACKDOOR_KEY = 'KEYCODE_CAMERA'
def snapshot(title):
  sleep()
  filename = title+'.png'
  print filename
  result = device.takeSnapshot()
  result.writeToFile(targetdir + filename,'png')

def sleep(duration=1):
  MonkeyRunner.sleep(duration)
  print "sleeping"

def back():
  device.press('KEYCODE_BACK')
  sleep()

def down():
  device.press('KEYCODE_DPAD_DOWN')

def right():
  device.press('KEYCODE_DPAD_RIGHT')

def left():
  device.press('KEYCODE_DPAD_LEFT')

#select the nth item from menu (0 indexed)
def menu(n):
  device.press('KEYCODE_MENU')
  sleep()
  for _ in range(10):
    up() #make sure we start from top
  for _ in range(n):
    down()
  enter()

def enter():
  device.press('KEYCODE_ENTER')
  sleep()

def up():
  device.press('KEYCODE_DPAD_UP')

def toTopLeft(force=5):
  for _ in range(force*2):
    up()
  for _ in range(force):
    left()

def toBottomLeft(force=5):
  for _ in range(force*2):
    down()
  for _ in range(force):
    left()

def finalize():
  back()
  back()
  back()

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()

# start
package = 'org.totschnig.myexpenses'
activity = 'org.totschnig.myexpenses.activity.MyExpenses'
runComponent = package + '/' + activity
#we start with the third account set up in fixture
extra = {'_id': "3"}
device.startActivity(component=runComponent,extras=extra)

def main():

  #1 ManageAccounts
  toTopLeft()
  enter()
  sleep()
  down()
  down()
  down()
  sleep()
  toTopLeft(8)
  snapshot("manage_accounts")
  
  if (stage == "1"):
    finalize()
    return
  
  #3 GrooupedList
  #back() does not close drawer but finishes activity?
  toTopLeft()
  enter()
  snapshot("grouped_list")
    
  #4 Templates and Plans
  toBottomLeft()
  right()
  right()
  right()
  enter() #temmplates button
  toTopLeft()
  down()
  right()
  enter() #plans tab
  sleep(2)
  down()
  enter() #openinstances
  down()
  device.press('KEYCODE_ENTER', MonkeyDevice.DOWN) # open CAB
  sleep()
  up()
  up()
  up()
  left()
  enter() #apply instance
  sleep(4)
  snapshot("plans")
  
  #5 ExportAndReset
  back()
  menu(1)
  snapshot("export")
  
  #6 Calculator
  back()
  toBottomLeft()
  sleep()
  right()
  sleep()
  enter()
  toTopLeft()
  activity = 'org.totschnig.myexpenses.activity.CalculatorInput'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  snapshot("calculator")
  
  #7 Split
  back()
  back()
  toTopLeft()
  down()
  down()#split is second, first is the transaction created from plan
  enter()
  sleep(2)
  right()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  snapshot("split")
  
  #8 Attach picture
  back()
  toTopLeft()
  down()
  down()
  down() #third in list
  enter()
  sleep(2)
  right()
  right()
  enter()
  #give time for loading
  sleep(2)
  back()#close virtual keyboard
  #navigate to imageView
  toTopLeft()
  down()
  down()
  down()
  down()
  down()
  down()
  right()
  right()
  enter()
  snapshot("attach_picture")
  
  #8 Distribution
  back()
  back()
  menu(2)
  right()
  right()
  enter()
  down()
  down()
  down()
  enter()
  sleep()
  snapshot("distribution")

  #11 Help
  back()
  activity = 'org.totschnig.myexpenses.activity.Help'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent)
  snapshot("help")
  
  #9 Backup
  back()
  activity = 'org.totschnig.myexpenses.activity.BackupRestoreActivity'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent,action="myexpenses.intent.backup")
  snapshot("backup")
  
  #10 Password
  back()
  activity = 'org.totschnig.myexpenses.activity.MyPreferenceActivity'
  runComponent = package + '/' + activity
  device.startActivity(component=runComponent,action="myexpenses.intent.preference.password")
  down()
  enter()
  enter()
  snapshot("password")

  #10 Light Theme
  back()
  back()
  device.startActivity(component=runComponent)
  for _ in range(5):
    down()
  enter()
  down()
  enter()
  back()
  snapshot("light_theme")

  finalize()

main()


