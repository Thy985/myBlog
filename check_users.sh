#!/bin/bash
mysql -uroot -p147258369Thy@ blog -e "SELECT id, username, email, status, is_deleted FROM t_user"
