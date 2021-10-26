using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;

namespace CounterFunctions
{
    public class availablePlaces : TableEntity
    {
        public String numOfPlaces { get; set; }
    }
}