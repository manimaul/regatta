#!/bin/sh -e

systemctl daemon-reload
systemctl enable regatta.service
systemctl start regatta.service
