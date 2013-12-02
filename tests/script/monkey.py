import sys

if (len(sys.argv) < 3):
  print "Usage: monkeyrunner monkey.py {lang} {country}"
  sys.exit(0)

lang = sys.argv[1]
country = sys.argv[2]
targetdir = '/home/michael/programmieren/MyExpenses/doc/screenshots/neu/' + lang + '/'
def snapshot(title):
  sleep()
  filename = title+'.png'
  print filename
  result = device.takeSnapshot()
  result.writeToFile(targetdir + filename,'png')

def sleep(duration=1):
  MonkeyRunner.sleep(duration)
  print "sleeping"

def down_and_up(key):
  device.press(key,MonkeyDevice.DOWN_AND_UP)

def back():
  down_and_up('KEYCODE_BACK')
  sleep()

def down():
  down_and_up('KEYCODE_DPAD_DOWN')

def right():
  down_and_up('KEYCODE_DPAD_RIGHT')

def left():
  down_and_up('KEYCODE_DPAD_LEFT')

#select the nth item from menu (0 indexed)
def menu(n):
  down_and_up('KEYCODE_MENU')
  sleep()
  for _ in range(10):
    up() #make sure we start from top
  for _ in range(n):
    down()
  enter()

def enter():
  down_and_up('KEYCODE_ENTER')
  sleep()

def up():
  down_and_up('KEYCODE_DPAD_UP')

def toOrigin():
  for _ in range(5):
    up()
    left()

from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
device = MonkeyRunner.waitForConnection()

# start
package = 'org.totschnig.myexpenses'
activity = 'org.totschnig.myexpenses.activity.MyExpenses'
runComponent = package + '/' + activity
extraDic = {} 
extraDic['instrument_language'] = lang 
extraDic['instrument_country'] = country 
device.startActivity(extras=extraDic,component=runComponent)

#1 ManageAccounts
left()
left()
enter()
snapshot("manage_accounts")

#2 AggregateDialog
right()
right()
enter()
down()
down()
snapshot("aggregate_dialog")

#3 GrooupedList
back()
toOrigin()
down()
enter()
snapshot("grouped_list")

#4 NewFromTemplate
menu(2)
down()
enter()
right()
right()
right()
enter()
for _ in range(6):
  down()
enter()
sleep(5)
snapshot("plan")

#5 ExportAndReset
back()
back()
back()
menu(1)
snapshot("export")

#6 Calculator
back()
toOrigin()
sleep()
right()
right()
sleep()
enter()
toOrigin()
activity = 'org.totschnig.myexpenses.activity.CalculatorInput'
runComponent = package + '/' + activity
device.startActivity(component=runComponent)
snapshot("calculator")

#7 Split
back()
back()
toOrigin()
down()
enter()
right()
enter()
#give time for loading
sleep(2)
snapshot("split")

#8 Distribution
back()
menu(5)
right()
enter()
down()
down()
down()
enter()
down() 
enter()
snapshot("distribution")

#9 Backup
back()
menu(6)
if lang == 'de':
  distance = 17
else:
  distance = 16
for _ in range(distance):
  down()
enter()
snapshot("backup")

#10 Password

back()
back()
menu(6)
if lang == 'de':
  distance = 24
else:
  distance = 23
for _ in range(distance):
  down()
enter()
enter()
snapshot("password")

#10 Light Theme
back()
back()
menu(6)
for _ in range(5):
  down()
enter()
down()
enter()
back()
snapshot("light_theme")

#11 Help
menu(7)
snapshot("help")
