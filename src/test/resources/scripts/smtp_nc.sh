#/bin/bash

while true
do
echo "nc -l 8025"
printf "220 smtp.localhost.local MyFake SMTP MAIL Service\n\
250 smtp.localhost.local HiThere\n\
250 2.1.0 OK\n\
250 2.1.5 OK\n\
354 Start mail input; end with <CRLF>.<CRLF>\n\
250 2.0.0 Ok: queued as ForGetIt\n\
221 2.0.0 Bye\n" | nc -l 8025

test $? -gt 128 && break;

done

