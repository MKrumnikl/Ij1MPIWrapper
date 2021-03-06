#!/bin/bash
FILES=./*.OU

# Create the temporary data file
touch temp_data.dat

# Column info: Number of nodes, Time in seconds
for f in $FILES
do
    time=$(ps -ef | awk '/^Total execution time/ {printf "%s",$4; exit;}' $f)
    nodes=$(ps -ef | awk '/^My rank/ {print $6; exit;}' $f)
    echo "${nodes//[!0-9,]} ${time//[!0-9.]}" >> temp_data.dat
done

# Sort the data to be able to calculate the media
sort -n temp_data.dat > data.dat

# Remove the temporary file with the unsorted data
rm temp_data.dat

# Calculate the median of each 10 elements
input="data.dat"
counter=0
firstElementCounter=5
secondElementCounter=6
firstElement=0
"" > median.dat
"" > box.dat
echo "Starting median calculation..."
while IFS= read -r line
do
	let "counter+=1"
	# Store the first middle element
	echo "$counter $line"
	if [ $counter == $firstElementCounter ]
	then
		wordCounter=0
		let "firstElementCounter+=10"
		for word in $line
		do
			let "wordCounter+=1"
			if [ $wordCounter == 2 ]
			then
				echo "the word is: $word"
				firstElement=$word
			fi
		done
	fi
	# Get the second middle element
	if [ $counter == $secondElementCounter ]
	then
		wordCounter=0
		let "secondElementCounter+=10"
		for word in $line
		do
			let "wordCounter+=1"
			if [ $wordCounter == 1 ]
			then 
			    nodes=$word
			fi
			if [ $wordCounter == 2 ]
			then
				echo "the second word is: $word"
				median=$(echo "scale=3; ($firstElement + $word)/2" | bc)
				echo "$nodes $median" >> median.dat
			fi
		done
	fi
	
	# Save the shorted values without the nodes
	wordCounter=0
	for word in $line
	do
		let "wordCounter+=1"
		if [ $wordCounter == 2 ]
		then
			echo "$word" >> box.dat
		fi
	done
done < "$input"
echo "Finished median calculation"

"" > boxData.dat
# Split to columns by 10 values
awk ' 
	{idx=FNR%10+1}
	!(idx in a){key[++key[0]]=idx}
	{a[idx]=a[idx] OFS $0}
	END {for(i=1;i<=key[0];i++) print a[key[i]]}' 'box.dat' >> boxData.dat
