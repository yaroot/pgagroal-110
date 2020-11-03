#!/usr/bin/env python3

from gevent import monkey

monkey.patch_all()

import os
import traceback
import random
import time
from gevent.pool import Pool
import pg8000
import logging

logging.basicConfig()

rand = random.Random(time.time())


POOLSIZE = int(os.environ.get('PG_POOLSIZE', '30'))
CONN_LIFETIME = int(os.environ.get('PG_CONN_LIFETIME', '60'))

DBHost = os.environ.get("PG_HOST", "postgres")
DBPort = int(os.environ.get("PG_PORT", "5555"))
DBName = os.environ.get("PG_DB", "test")
DBUser = os.environ.get("PG_USER", "test")
DBPasswd = os.environ.get("PG_PASSWD", "test")


# sleep 0.1~2s
def sleep1():
    return rand.randint(100, 2000) / 1000


def test(n: int):
    logging.warning('Start %d', n)
    start = time.monotonic()
    with pg8000.connect(
            user=DBUser,
            password=DBPasswd,
            database=DBName,
            host=DBHost,
            port=DBPort,
    ) as conn:
        try:
            conn.autocommit = False
            while time.monotonic() - start < CONN_LIFETIME:
                conn.run('select pg_sleep(%s)' % sleep1())
                time.sleep(0.01)
                conn.commit()
        except Exception:
            conn.rollback()
            logging.exception('Error %d', n)
    logging.warning('Complete %d', n)
    time.sleep(sleep1())
    pass


def main():
    logging.info(f'Connecting to postgres://{DBUser}:{DBPasswd}@{DBHost}:{DBPort}/{DBName} '
                 f'(pool_size={POOLSIZE}, max_lifetime={CONN_LIFETIME})')
    pool = Pool(POOLSIZE)
    i = 0
    while True:
        pool.spawn(test, i)
        i += 1
    pool.join()


if __name__ == '__main__':
    main()
