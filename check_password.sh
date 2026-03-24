#!/bin/bash
mysql -uroot -p147258369Thy@ blog -e "SELECT id, username, password FROM t_user WHERE username='admin'"
