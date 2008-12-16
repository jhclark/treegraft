NAME=treegraft-prerelease-2008-10-07

cp ../lib/JonClarkDotInfo.jar ../lib/Taru.jar .
mv Taru.jar taru.jar

tar -cvzf $NAME.tar.gz treegraft.properties treegraft_dist.sh taru_treegraft_dist.sh treegraft_dist.sh taru.jar JonClarkDotInfo.jar treegraft.jar taru_treegraft.properties treegraft.properties mert.sh

rm JonClarkDotInfo.jar taru.jar