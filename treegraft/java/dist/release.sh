NAME=treegraft-prerelease-2008-08-19

cp ../lib/JonClarkDotInfo.jar ../lib/Taru.jar .
mv Taru.jar taru.jar

tar -cvzf $NAME.tar.gz taru_treegraft_dist.sh taru.jar JonClarkDotInfo.jar treegraft.jar taru_treegraft.properties

rm JonClarkDotInfo.jar taru.jar