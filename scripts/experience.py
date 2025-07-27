import math
base=5
classes={'common':1.5,'uncommon':2.0,'rare':3.0,'epic':4.0,'legendary':6.0,'mythic':8.0}
max_scale=320
power=2.0
offset=1000
levels=[1,10,20,30,40,50,70,100]
print('Lvl\t'+'\t'.join(classes.keys()))
for lvl in levels:
 prog=lvl**power/(offset+lvl**power)
 scale=1+(max_scale-1)*prog
 row=[lvl]
 for cm in classes.values():
  row.append(round(base*cm*scale))
 print('\t'.join(map(str,row)))