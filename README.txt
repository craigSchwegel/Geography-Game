
<h1>Geography Game</h1>
It's the end of the summer and school is just around the corner.
This is the time when we (and many other families) decide to take
a farewell-to-summer road trip. Hours in the car and children mix
like functional programming and mutable state or productivity and
open floor plans, so we need to find ways of keeping them
entertained that don't involve pushing each other out of a moving
vehicle, dead arms or spitting.
<p>
One of the games we like to play is called "Geography." The rules
are simple: one person names a city and the next person has to
name another city that begins with the last letter of his/her
opponent's city. So if I say "Newark", you can say "Kansas City"
and then I can say "Youngstown" and you can say "Noxville" and
then you lose because it's "Knoxvsille" you ignoramus! Eventually,
someone gets stuck and loses the game, which sometimes results in
crying and fighting.
</p>
<p>
I hate losing games, so I want to write a program to give me the
best chance of winning. Assume you have a dataset of all major
world cities like this one.
<a href="https://datahub.io/core/world-cities>Major Cities</a>
</p>
<p>
Your goal is to implement the function
<b>def nextMove(opponentsCity: String) : String</b>
which, given any city, will respond with your next move. Note
that your goal is not simply to play the game but to maximize your
chances of winning it. Also note that you will lose if you repeat
a city or use a city for which there is no valid response.
</p>
 *
<b>Solution:</b>
The program loads the list of cities in a Trie structure partitioned
by the last letter of the city name.  Partitioning by last letter
allows us to select a city name with the fewest responses.  In addition,
we need to keep track of the count of cities that start with a letter and
sort that list to efficiently look up the next city with the fewest
responses.