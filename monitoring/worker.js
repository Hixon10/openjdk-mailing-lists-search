async function checkIndexerState(e) {
     const init = {
       headers: {
         'Cache-Control': 'no-cache',
		 'pragma': 'no-cache'
       },
     };
     const statResponse = await fetch("https://hixon10.github.io/openjdk-mailing-lists-search/dbsize.txt", init);
	 const fileStat = await statResponse.text();
	 const parts = fileStat.split('|');
	 const currentSize = parts[0];
	 const changeTimestampInSec = parts[1];
	 const currentTimestampInSec = new Date() / 1000;
	 
	 if (currentTimestampInSec - changeTimestampInSec < 432000) { // 432000 == 5 days is OK for us
		console.log("Everything is ok");
		 return;
	 }
	  
    const send_request = new Request('https://api.mailchannels.net/tx/v1/send', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
      },
      body: JSON.stringify({
        personalizations: [
          {
            to: [{ email: 'TO_EMAIL', name: 'Denis' }],
          },
        ],
        from: {
          email: 'FROM_EMAIL',
          name: 'Denis',
        },
        subject: '[ALERT] Update OpenJDK Mailing lists index',
        content: [
          {
            type: 'text/plain',
            value: 'You have outdated index: ' + fileStat,
          },
        ],
      }),
    });
	console.log("send alert");
	const result = await fetch(send_request);
}

addEventListener("scheduled", (event) => {
  event.waitUntil(checkIndexerState(event));
});
  
  
