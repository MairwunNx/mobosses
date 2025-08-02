import math
base=5.0
classes={'common':1.2,'uncommon':1.4,'rare':1.7,'epic':2.0,'legendary':2.3,'mythic':2.6}
max_scale=2.0
power=1.2
offset=180.0
flatten=0.35
cap=28
levels=[1,10,20,30,40,50,70,100]

print('Lvl\t'+'\t'.join(classes.keys()))
for lvl in levels:
    lp=lvl**power
    prog=lp/(offset+lp)
    scale=1+(max_scale-1)*prog
    row=[lvl]
    for cm in classes.values():
        cm_eff=1+(cm-1)*(1-flatten*prog)
        val=round(min(base*cm_eff*scale, cap))
        row.append(val)
    print('\t'.join(map(str,row)))