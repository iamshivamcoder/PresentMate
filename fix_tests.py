import os
import glob
import re

base_dir = r'f:\AndroidStudioProjects\PresentMate\app\src\test\java\com\example\presentmate'

# Fix SavedPlaceDaoTest
f1 = os.path.join(base_dir, 'data', 'SavedPlaceDaoTest.kt')
with open(f1, 'r') as f:
    c = f.read()
c = c.replace('dao.getByName("Home")', 'dao.getByName("Home", "test_user")')
c = c.replace('dao.getByName("Work")', 'dao.getByName("Work", "test_user")')
c = c.replace('dao.getAll()', 'dao.getAll("test_user")')
with open(f1, 'w') as f:
    f.write(c)

# Fix StudySessionLogDaoTest
f2 = os.path.join(base_dir, 'db', 'StudySessionLogDaoTest.kt')
with open(f2, 'r') as f:
    c = f.read()
c = c.replace('dao.getByEventId(1L)', 'dao.getByEventId(1L, "test_user")')
c = re.sub(r'dao\.getById\((.*?)\)', r'dao.getById(\1, "test_user")', c)
c = c.replace('dao.getPendingOverdue(3000L)', 'dao.getPendingOverdue(3000L, "test_user")')
with open(f2, 'w') as f:
    f.write(c)

# Fix GeofenceBroadcastReceiverTest
f3 = os.path.join(base_dir, 'geofence', 'GeofenceBroadcastReceiverTest.kt')
with open(f3, 'r') as f:
    c = f.read()
c = c.replace('attendanceDao.getOngoingSessionFlow()', 'attendanceDao.getOngoingSessionFlow(any())')
with open(f3, 'w') as f:
    f.write(c)

# Fix OverviewScreenTest
f4 = os.path.join(base_dir, 'ui', 'screens', 'OverviewScreenTest.kt')
with open(f4, 'r') as f:
    c = f.read()
c = c.replace('attendanceDao.getAllRecords()', 'attendanceDao.getAllRecords(any())')
c = c.replace('attendanceDao.getAllRecordsNonFlow()', 'attendanceDao.getAllRecordsNonFlow(any())')
with open(f4, 'w') as f:
    f.write(c)

# Fix SettingsScreenTest
f5 = os.path.join(base_dir, 'ui', 'screens', 'SettingsScreenTest.kt')
with open(f5, 'r') as f:
    c = f.read()
c = c.replace('attendanceDao.getAllDeletedRecords()', 'attendanceDao.getAllDeletedRecords(any())')
c = c.replace('attendanceDao.getAllDeletedRecords(any()).collect', 'attendanceDao.getAllDeletedRecords("test_user").collect')
with open(f5, 'w') as f:
    f.write(c)

# Fix AttendanceViewModelTest
f6 = os.path.join(base_dir, 'viewmodel', 'AttendanceViewModelTest.kt')
with open(f6, 'r') as f:
    c = f.read()
c = c.replace('attendanceDao.getOngoingSession()', 'attendanceDao.getOngoingSession(any())')
c = c.replace('attendanceDao.getOngoingSessionFlow()', 'attendanceDao.getOngoingSessionFlow(any())')
c = c.replace('attendanceDao.getAllRecords()', 'attendanceDao.getAllRecords(any())')
with open(f6, 'w') as f:
    f.write(c)

# Fix OverviewViewModelTest
f7 = os.path.join(base_dir, 'viewmodel', 'OverviewViewModelTest.kt')
with open(f7, 'r') as f:
    c = f.read()
c = c.replace('attendanceDao.getAllRecords()', 'attendanceDao.getAllRecords(any())')
c = c.replace('attendanceDao.getAllRecordsNonFlow()', 'attendanceDao.getAllRecordsNonFlow(any())')
with open(f7, 'w') as f:
    f.write(c)

print('Done fixing test files.')
