using System;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;
using Microsoft.Extensions.Logging;
using Microsoft.WindowsAzure.Storage.Table;
using Newtonsoft.Json;
using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Auth;
using Microsoft.Azure.Devices;
using System.Collections.Generic;

namespace CounterFunctions
{

    public static class CounterFunctions
    {
        //add
        static RegistryManager registryManager;
     //   private static string connectionString = "HostName=FirstTry1.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=pM+0OHYzhamTy1GSRFfasj7q7Hi2TARGlVr9yb7dNuk=";
        private static string ownerName = "";
        //end_add
        private static readonly AzureSignalR SignalR = new AzureSignalR(Environment.GetEnvironmentVariable("AzureSignalRConnectionString"));
        //  static string accountName = "firsttry1";
        //  static string accountKey = "pfXP7PSpVukhCQmIKLv44hRo93hnZWuyt3D/TVL5+ImwIeXX0BAOlMvhsBV96eD5rbS465e8I/6JgQmsV4tlzg==";
           [FunctionName("negotiate")]
           public static async Task<SignalRConnectionInfo> NegotiateConnection(
               [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = null)] HttpRequestMessage request,
               ILogger log)
           {
               try
               {
                   ConnectionRequest connectionRequest = await ExtractContent<ConnectionRequest>(request);
                   log.LogInformation($"Negotiating connection for user: <{connectionRequest.UserId}>.");

                   string clientHubUrl = SignalR.GetClientHubUrl("CounterHub");
                   string accessToken = SignalR.GenerateAccessToken(clientHubUrl, connectionRequest.UserId);

                   return new SignalRConnectionInfo { AccessToken = accessToken, Url = clientHubUrl };
               }
               catch (Exception ex)
               {
                   log.LogError(ex, "Failed to negotiate connection.");
                   throw;
               }
           }
        [FunctionName("get-isOpen")]
        public static async Task<status> GetIfIsOpen(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-isOpen/{id}/{state}")] HttpRequestMessage request,
            string id,
            string state,
            [SignalR(HubName = "CounterHub")] IAsyncCollector<SignalRMessage> signalRMessages,
            ILogger log)
        {
            log.LogInformation("Getting if to open or not.");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;

            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Users");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            status st = new status();

            CloudTable garageTable = client.GetTableReference("Garage");
            Boolean isInTheGarage = await isIdInTable(garageTable, "PartitionKey", id);
            /*get the avaliable places*/
            TableQuery<availablePlaces> idQuery = new TableQuery<availablePlaces>()
               .Where(TableQuery.GenerateFilterCondition("RowKey", QueryComparisons.Equal, "places"));
            TableQuerySegment<availablePlaces> queryResult = await garageTable.ExecuteQuerySegmentedAsync(idQuery, null);
            availablePlaces places = queryResult.FirstOrDefault();

            if (isInTheGarage && state.Equals("in"))
            {
                st.isOpen = "arleady in the Garage!";
                return st;
            }
            else if (!isInTheGarage && state.Equals("out"))
            {
                st.isOpen = "arleady out of the Garage!";
                return st;
            }
            else if (isInTheGarage && state.Equals("out"))
            {
                //remove car from garage
                TableOperation retrieve = TableOperation.Retrieve<Request>(id, "car");
                TableResult result = await garageTable.ExecuteAsync(retrieve);
                var deleteEntity = (TableEntity)result.Result;
                TableOperation delete = TableOperation.Delete(deleteEntity);
                await garageTable.ExecuteAsync(delete);

                st.isOpen = "open";
                //add one to the avaliavle places
                places.numOfPlaces = (int.Parse(places.numOfPlaces)+1).ToString();
                //signalR
                await signalRMessages.AddAsync(
                  new SignalRMessage
                  {
                      Target = "placesUpdate",
                      Arguments = new [] { places.numOfPlaces }
                  });
                await signalRMessages.AddAsync(
                  new SignalRMessage
                  {
                      Target = "garageUpdate",
                      Arguments = new[] { (Object)id }
                  });
                await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = ((Request)result.Result).UserName+"Notify",
                     Arguments = new[] { ((Request)result.Result).ownerName+" left the garage" }
                 });
                await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = "adminNotify",
                     Arguments = new[] { ((Request)result.Result).ownerName + " left the garage" }
                 });

                TableOperation add = TableOperation.InsertOrReplace(places);
                await garageTable.ExecuteAsync(add);
                return st;
            }
            //in
            if (int.Parse(places.numOfPlaces) == 0) {
                st.isOpen = "don't open the Garage is FULL!";
                return st;
            }
            List<String> users = await GetUsersFromTable(table);
            foreach (string regesterdUser in users)
            {

                CloudTable usersTable = client.GetTableReference("Table00" + regesterdUser);
                await usersTable.CreateIfNotExistsAsync();
                Boolean isIdRegestered = await isIdInTable(usersTable, "RowKey", id);
                if (isIdRegestered)
                {
                    //add to garage
                    Request newCar = new Request();
                    newCar.PartitionKey = id;
                    newCar.RowKey = "car";
                    newCar.ownerName = ownerName;
                    newCar.UserName = regesterdUser;

                    TableOperation add = TableOperation.InsertOrReplace(newCar);
                    await garageTable.ExecuteAsync(add);
                    st.isOpen = "open";
                    //update the avaliable places
                    places.numOfPlaces = (int.Parse(places.numOfPlaces) - 1).ToString();
                    await signalRMessages.AddAsync(
                     new SignalRMessage
                     {
                         Target = "placesUpdate",
                         Arguments = new[] { places.numOfPlaces }
                     }) ;
                    await signalRMessages.AddAsync(
                  new SignalRMessage
                  {
                      Target = "garageUpdate",
                      Arguments = new[] { (Object)id }
                  });
                    await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = regesterdUser + "Notify",
                     Arguments = new[] { ownerName + " entered the garage" }
                 });
                    await signalRMessages.AddAsync(
                new SignalRMessage
                {
                    Target = "adminNotify",
                    Arguments = new[] { ownerName + " entered the garage" }
                });
                    TableOperation addOrReplace = TableOperation.InsertOrReplace(places);
                    await garageTable.ExecuteAsync(addOrReplace);
                    return st;
                }
            }
            st.isOpen = "don't open";
            return st;
        }
        private static async Task<Boolean> isIdInTable(CloudTable table, String colName, string platId)
        {
            TableQuery<PlateNumber> idQuery = new TableQuery<PlateNumber>()
               .Where(TableQuery.GenerateFilterCondition(colName, QueryComparisons.Equal, platId));
            TableQuerySegment<PlateNumber> queryResult = await table.ExecuteQuerySegmentedAsync(idQuery, null);
            PlateNumber plateNumber = queryResult.FirstOrDefault();
            if (plateNumber == null)
            {
                return false;
            }
            ownerName = plateNumber.PartitionKey;
            return true;
        }

        [FunctionName("update-User")]
        public static async Task<String> updateUser(
          [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "update-User/{act}/{name}")] HttpRequestMessage request,
          [Table("Users")] CloudTable cloudTable,
          string name,
          string act,
          ILogger log)
        {
            Console.Out.WriteLine("in updateUser");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();
                table = client.GetTableReference("Table00" + name);

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            if (act.Equals("remove"))
            {
                Console.Out.WriteLine("in remove");
                //delete table
                await table.DeleteIfExistsAsync();
                //delete user from Users
                TableOperation retrieve = TableOperation.Retrieve<TableEntity>(name, "");
                CloudTable usersTable = client.GetTableReference("Users");
                await usersTable.CreateIfNotExistsAsync();
                TableResult result = await usersTable.ExecuteAsync(retrieve);

                var deleteEntity = (TableEntity)result.Result;

                TableOperation delete = TableOperation.Delete(deleteEntity);

                await usersTable.ExecuteAsync(delete);
                //delete requests
                CloudTable requestTable = client.GetTableReference("Requests");
                TableQuery<Request> idQuery = new TableQuery<Request>()
               .Where(TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, name));
                TableQuerySegment<Request> queryResult = await requestTable.ExecuteQuerySegmentedAsync(idQuery, null);
                var batchOperation = new TableBatchOperation();
                foreach (var e in queryResult.Results) {
                    batchOperation.Delete((TableEntity)e);
                }
                if((queryResult.Results).Count != 0) { 
                    await requestTable.ExecuteBatchAsync(batchOperation);
                }

                return act + " " + name;

            }
            else if (act == "add")
            {
                Console.Out.WriteLine("in add");
                await table.CreateIfNotExistsAsync();
                CloudTable usersTable = client.GetTableReference("Users");
                await usersTable.CreateIfNotExistsAsync();

                User newUser = new User();
                newUser.PartitionKey = name;
                newUser.RowKey = "";
                newUser.Password = name;
                newUser.UserType = "user";

                TableOperation add = TableOperation.InsertOrReplace(newUser);
                await usersTable.ExecuteAsync(add);

                return act + " " + name;

            }
            return act + " " + name + " error in action";

        }

        [FunctionName("get-All-Users")]
        public static async Task<List<string>> GetUsers(
           [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-All-Users/")] HttpRequestMessage request,
           [Table("Users")] CloudTable cloudTable,

           ILogger log)
        {
            Console.WriteLine("in get-All-Users");
            //addition
            CloudTable table = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                CloudTableClient client = account.CreateCloudTableClient();

                table = client.GetTableReference("Users");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            Console.WriteLine("callin GetUsersFromTable");
            return await GetUsersFromTable(table);
        }
        private static async Task<List<string>> GetUsersFromTable(CloudTable cloudTable)
        {
            TableQuery<TableEntity> idQuery = new TableQuery<TableEntity>();
            List<string> users = new List<string>();
            foreach (TableEntity entity in await cloudTable.ExecuteQuerySegmentedAsync(idQuery, null))
            {
                TableEntity user = new TableEntity();
                user.PartitionKey = entity.PartitionKey;

                users.Add(user.PartitionKey);
            }
            return users;
        }




        [FunctionName("add-PlateNumber")]
        public static async Task<String> GetaddPlateNymber(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "add-PlateNumber/{user}/{id}/{owner}")] HttpRequestMessage request,
            string id,
            string user,
            string owner,
            ILogger log)
        {
            log.LogInformation("add PlateNumber.");
            //addition
            CloudTable table = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                CloudTableClient client = account.CreateCloudTableClient();

                table = client.GetTableReference("Table00" + user);
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            return await updatePlateNumber(table, id, owner, "add");
        }

        [FunctionName("remove-PlateNumber")]
        public static async Task<String> GetRemovePlateNymber(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "remove-PlateNumber/{user}/{id}/{owner}")] HttpRequestMessage request,
            string id,
            string user,
            string owner,
            ILogger log)
        {
            log.LogInformation("remove PlateNumber.");
            //addition
            CloudTable table = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                CloudTableClient client = account.CreateCloudTableClient();

                table = client.GetTableReference("Table00" + user);
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            return await updatePlateNumber(table, id, owner, "remove");
        }


        private static async Task<String> updatePlateNumber(CloudTable cloudTable, string id, string owner, string action)
        {
            if (action.Equals("add"))
            {
                try
                {
                    PlateNumber newPlate = new PlateNumber();
                    newPlate.PartitionKey = owner;
                    newPlate.RowKey = id;

                    TableOperation add = TableOperation.InsertOrReplace(newPlate);
                    await cloudTable.ExecuteAsync(add);
                }
                catch (Exception e)
                {
                    return e.Message;
                }
                return "Success";
                // action=='remove'
            }
            else
            {
                try
                {
                    List<string> list = new List<string>();
                    list.Add(id);
                    TableOperation retrieve = TableOperation.Retrieve<PlateNumber>(owner, id);

                    TableResult result = await cloudTable.ExecuteAsync(retrieve);

                    var deleteEntity = (PlateNumber)result.Result;

                    TableOperation delete = TableOperation.Delete(deleteEntity);

                    await cloudTable.ExecuteAsync(delete);
                }
                catch (Exception e)
                {
                    return e.Message;
                }
                return "Success";
            }
        }
        [FunctionName("get-regestered-cars")]
        public static async Task<List<User>> GetRegesteredCar(
           [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-regestered-cars/{user}")] HttpRequestMessage request,
           string user,
           ILogger log)
        {
            
            CloudTable table = null;
            CloudTableClient client = null;
            //first get all users
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Users");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            List<string> users;
            if (user.Equals("admin"))
            { //must be chaaaaaaange
                users = await GetUsersFromTable(table);
            }
            else
            {
                users = new List<string>();
                users.Add(user);
            }

            List<User> cars = new List<User>();
            foreach (string regesterdUser in users)
            {
                CloudTable usersTable = client.GetTableReference("Table00" + regesterdUser);
                await usersTable.CreateIfNotExistsAsync();
                TableQuery<User> idQuery = new TableQuery<User>();
                foreach (User entity in await usersTable.ExecuteQuerySegmentedAsync(idQuery, null))
                {
                    entity.UserName = regesterdUser;
                    cars.Add((User)entity);
                }
            }

            return cars;
        }

        [FunctionName("post-login")]
        public static async Task<String> isLogin(
           [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = null)] HttpRequestMessage request,
           ILogger log)
        {
            log.LogInformation("is login.");
            String result = "false";
            CloudTable table = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                CloudTableClient client = account.CreateCloudTableClient();

                table = client.GetTableReference("Users");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            User userLoginRequest = await ExtractContent<User>(request);

            TableQuery<User> idQuery = new TableQuery<User>()
               .Where(TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, userLoginRequest.PartitionKey));
            TableQuerySegment<User> queryResult = await table.ExecuteQuerySegmentedAsync(idQuery, null);
            User user = queryResult.FirstOrDefault();

            if (user == null)
            {
                result = "false";
            }
            else
            {
                if (user.Password.Equals(userLoginRequest.Password))
                {
                    result = user.UserType;
                }
                else { result = "false"; }
            }
            return result;

        }
        [FunctionName("post-changePass")]
        public static async Task<String> changePass(
           [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = null)] HttpRequestMessage request,
           ILogger log)
        {
            log.LogInformation("is login.");
            String result = "false";
            CloudTable table = null;
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                CloudTableClient client = account.CreateCloudTableClient();

                table = client.GetTableReference("Users");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            User userLoginRequest = await ExtractContent<User>(request);

            TableQuery<User> idQuery = new TableQuery<User>()
               .Where(TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, userLoginRequest.PartitionKey));
            TableQuerySegment<User> queryResult = await table.ExecuteQuerySegmentedAsync(idQuery, null);
            User user = queryResult.FirstOrDefault();

            if (user == null)
            {
                result = "notChanged";
            }
            else
            {
                user.Password = userLoginRequest.Password;

                TableOperation add = TableOperation.InsertOrReplace(user);
                await table.ExecuteAsync(add);
                result = "changed";
            }
            return result;

        }
        private static async Task<T> ExtractContent<T>(HttpRequestMessage request)
        {
            string connectionRequestJson = await request.Content.ReadAsStringAsync();
            Console.Out.Write("the connectionRequestJson is :   " + connectionRequestJson + "\n");
            return JsonConvert.DeserializeObject<T>(connectionRequestJson);
        }

        [FunctionName("get-requests")]
        public static async Task<List<Request>> GetRequests(
           [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-requests/{user}/{isApproved}")] HttpRequestMessage request,
           string user,
           string isApproved,
           ILogger log)
        {
            log.LogInformation("GetRequests");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;
            Boolean isAdmin = false;


            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Requests");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            if (user.Equals("admin"))
            { //must be chaaaaaaange
                isAdmin = true;
            }

            return await GetRequests(table, user, isAdmin, isApproved);
        }
        private static async Task<List<Request>> GetRequests(CloudTable cloudTable, string userName, Boolean isAdmin, string isApproved)
        {
            string idQuery = null;

            List<Request> requests = new List<Request>();
            if (isAdmin)
            {
                idQuery = TableQuery.GenerateFilterCondition("approved", QueryComparisons.Equal, "waiting");

            }
            else
            {
                if (isApproved.Equals("true"))
                {
                    idQuery = TableQuery.CombineFilters(
                    TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, userName),
                    TableOperators.And,
                    TableQuery.GenerateFilterCondition("approved", QueryComparisons.NotEqual, "waiting"));

                    string deleteQuery = TableQuery.CombineFilters(
                        idQuery,
                       TableOperators.And,
                       TableQuery.GenerateFilterConditionForDate("Timestamp", QueryComparisons.LessThanOrEqual, DateTimeOffset.Now.AddDays(-1).Date));
                    TableQuery<Request> deleteRequestQuery = new TableQuery<Request>().Where(deleteQuery);
                    TableQuerySegment<Request> queryDelteResult = await cloudTable.ExecuteQuerySegmentedAsync(deleteRequestQuery, null);
                    foreach (TableEntity entity in queryDelteResult.Results)
                    {
                        TableOperation delete = TableOperation.Delete(entity);
                        await cloudTable.ExecuteAsync(delete);
                    }
                }
                else
                {
                    idQuery = TableQuery.CombineFilters(
                   TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, userName),
                   TableOperators.And,
                   TableQuery.GenerateFilterCondition("approved", QueryComparisons.Equal, "waiting"));
                }
            }

            TableQuery<Request> query = new TableQuery<Request>().Where(idQuery);
            var results = await cloudTable.ExecuteQuerySegmentedAsync<Request>(query, null);

            List<Request> cloudRequest = results.Results;

            return cloudRequest;
        }
        [FunctionName("get-action-request")]
        public static async Task<string> GetActionRequest(//*
           [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-action-request/{act}/{user}/{owner}/{plateNum}")] HttpRequestMessage request,
           string user,
           string act,
           string owner,
           string plateNum,
            [SignalR(HubName = "CounterHub")] IAsyncCollector<SignalRMessage> signalRMessages,
           ILogger log)
        {
            log.LogInformation("GetActionRequest");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;

            //first get all users
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Requests");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            if (act.Equals("add"))
            {
                //check if it's a new request 
                List<Request> requestList = await GetRequests(table, user, true, null);
                string idQuery = TableQuery.GenerateFilterCondition("RowKey", QueryComparisons.Equal, plateNum);
                TableQuery<Request> query = new TableQuery<Request>().Where(idQuery);
                var results = await table.ExecuteQuerySegmentedAsync<Request>(query, null);
                foreach (Request e in results.Results) {
                    if (e.approved.Equals("waiting")) {
                        if (e.PartitionKey.Equals(user))
                        {
                            return "Failed : the Request arleady suggested ";
                        }
                        else {
                            return "Failed : the Request arleady suggested by another user";
                        }
                    }
                }
                //check if the car arleady regestered 
                List<User> cars = await GetRegesteredCar(null,"admin",null);
                foreach (User e in cars)
                {
                    if (e.RowKey.Equals(plateNum)) {
                        if (e.UserName.Equals(user))
                            {
                            return "Failed : the car arleady registered by you ";
                        }else{
                            return "Failed : the car arleady registered by another user ";
                        }
                    }
                    
                }
                Request newRequest = new Request();
                newRequest.PartitionKey = user;
                newRequest.RowKey = plateNum;
                newRequest.ownerName = owner;
                newRequest.approved = "waiting";

                TableOperation add = TableOperation.InsertOrReplace(newRequest);
                await table.ExecuteAsync(add);
                await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = "requestUpdate",
                     Arguments = new[] { "add"}
                 });
                await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = "AddRequestNotify",
                     Arguments = new[] { user }
                 });

            }
            else
            {//remove 
                TableOperation retrieve = TableOperation.Retrieve<Request>(user, plateNum);

                TableResult result = await table.ExecuteAsync(retrieve);

                var deleteEntity = (Request)result.Result;

                TableOperation delete = TableOperation.Delete(deleteEntity);

                await table.ExecuteAsync(delete);
                await signalRMessages.AddAsync(
               new SignalRMessage
               {
                   Target = "requestUpdate",
                   Arguments = new[] { "remove:"+plateNum }
               });
                await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = "RemoveRequestNotify",
                     Arguments = new[] { user }
                 });

            }

            return act;
        }
        [FunctionName("get-approve-request")] //if true must update the user table 
        public static async Task<string> GetupdateRequest(//if we have to requests that contains the same id number??? //*
          [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-approve-request/{act}/{user}/{owner}/{plateNum}")] HttpRequestMessage request,
          string user,
          string act,
          string owner,
          string plateNum,
           [SignalR(HubName = "CounterHub")] IAsyncCollector<SignalRMessage> signalRMessages,
          ILogger log)
        {
            log.LogInformation("GetActionRequest");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;

            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Requests");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            Request newRequest = new Request();
            newRequest.PartitionKey = user;
            newRequest.RowKey = plateNum;
            newRequest.ownerName = owner;
            newRequest.approved = act;

            TableOperation add = TableOperation.InsertOrReplace(newRequest);
            await table.ExecuteAsync(add);
            await signalRMessages.AddAsync(
                 new SignalRMessage
                 {
                     Target = user + "NotifyFeeds",
                     Arguments = new[] { "emptyMassage" }
                 });
            if (act.Equals("true"))
            { //add the car

                table = client.GetTableReference("Table00" + user);
                await table.CreateIfNotExistsAsync();



                await updatePlateNumber(table, plateNum, owner, "add");
            }

            return "done";
        }
        [FunctionName("get-garage-content")] //if true must update the user table 
        public static async Task<List<Request>> GetGarageNow(//if we have to requests that contains the same id number???
         [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-garage-content/")] HttpRequestMessage request,
         ILogger log)
        {
            CloudTable table = null;
            CloudTableClient client = null;

            //first get all users
            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Garage");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }
            TableQuery<Request> idQuery = new TableQuery<Request>()
              .Where(TableQuery.GenerateFilterCondition("RowKey", QueryComparisons.Equal, "car"));
            TableQuerySegment<Request> queryResult = await table.ExecuteQuerySegmentedAsync(idQuery, null);
           
            return queryResult.Results;

        }
        [FunctionName("get-places-number")]
        public static async Task<int> GetNumberOfPlaces(
         [HttpTrigger(AuthorizationLevel.Anonymous, "get", Route = "get-places-number/")] HttpRequestMessage request,
         ILogger log)
        {
            log.LogInformation("GetNumberOfPlaces");
            //addition
            CloudTable table = null;
            CloudTableClient client = null;

            try
            {
                StorageCredentials creds = new StorageCredentials(Environment.GetEnvironmentVariable("accountName"), Environment.GetEnvironmentVariable("accountKey"));
                CloudStorageAccount account = new CloudStorageAccount(creds, useHttps: true);

                client = account.CreateCloudTableClient();

                table = client.GetTableReference("Garage");
                await table.CreateIfNotExistsAsync();

                Console.WriteLine(table.Uri.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            /*get the avaliable places*/
            TableQuery<availablePlaces> idQuery = new TableQuery<availablePlaces>()
               .Where(TableQuery.GenerateFilterCondition("RowKey", QueryComparisons.Equal, "places"));
            TableQuerySegment<availablePlaces> queryResult = await table.ExecuteQuerySegmentedAsync(idQuery, null);
            availablePlaces places = queryResult.FirstOrDefault();
            Console.Out.WriteLine("RowKey"+ places.RowKey);
            return int.Parse(places.numOfPlaces);


        }
    }


    }
    

